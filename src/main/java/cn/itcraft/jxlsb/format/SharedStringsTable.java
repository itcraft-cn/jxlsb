package cn.itcraft.jxlsb.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

public final class SharedStringsTable {
    
    private static final byte[] SST_FLAGS_ZERO = {0};
    
    private final List<String> strings = new ArrayList<>();
    private final ConcurrentHashMap<String, Integer> indexMap = new ConcurrentHashMap<>(1024);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    
    public int addString(String str) {
        totalCount.incrementAndGet();
        
        return indexMap.computeIfAbsent(str, k -> {
            int newIndex;
            synchronized (strings) {
                newIndex = strings.size();
                strings.add(k);
            }
            return newIndex;
        });
    }
    
    public int getCount() {
        return strings.size();
    }
    
    public int getTotalCount() {
        return totalCount.get();
    }
    
    public String getString(int index) {
        return strings.get(index);
    }
    
    public int size() {
        return strings.size();
    }
    
    /**
     * 从sharedStrings.bin加载字符串表
     * 
     * @param inputStream sharedStrings.bin输入流
     * @throws IOException IO异常
     */
    public void load(InputStream inputStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int bytesRead;
            int offset = 0;
            
            while ((bytesRead = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += bytesRead;
                
                int processed = processBuffer(buffer, offset);
                
                if (processed < offset) {
                    int remaining = offset - processed;
                    if (remaining > 0 && remaining < buffer.length) {
                        System.arraycopy(buffer, processed, buffer, 0, remaining);
                    }
                    offset = remaining;
                } else {
                    offset = 0;
                }
            }
            
            if (offset > 0) {
                processBuffer(buffer, offset);
            }
        } finally {
            inputStream.close();
        }
    }
    
    private int processBuffer(byte[] buffer, int length) {
        int pos = 0;
        
        while (pos + 2 <= length) {
            int recordType = VarIntReader.readVarInt(buffer, pos);
            int typeSize = VarIntReader.varIntSize(recordType);
            pos += typeSize;
            
            if (pos >= length) return pos - typeSize;
            
            int recordSize = VarIntReader.readVarSize(buffer, pos);
            int sizeBytes = VarIntReader.varSizeSize(recordSize);
            pos += sizeBytes;
            
            if (recordSize < 0) {
                pos += Math.max(0, recordSize);
                continue;
            }
            
            if (pos + recordSize > length) {
                return pos - typeSize - sizeBytes;
            }
            
            if (recordType == Biff12RecordType.BrtSSTItem) {
                String text = parseSSTItem(buffer, pos, recordSize);
                strings.add(text);
            }
            
            pos += recordSize;
        }
        
        return pos;
    }
    
    private String parseSSTItem(byte[] buffer, int offset, int size) {
        int flags = buffer[offset];
        int strOffset = offset + 1;
        
        int length = VarIntReader.readIntLE(buffer, strOffset);
        if (length == 0) {
            return "";
        }
        
        byte[] strBytes = new byte[length * 2];
        System.arraycopy(buffer, strOffset + 4, strBytes, 0, Math.min(strBytes.length, size - 5));
        
        return new String(strBytes, StandardCharsets.UTF_16LE);
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        Biff12Writer w = new Biff12Writer();
        
        // BrtBeginSst
        // 结构：cTotals(4) + cUnique(4)
        w.writeRecordHeader(Biff12RecordType.BrtBeginSst, 8);
        w.writeIntLE(totalCount.get());
        w.writeIntLE(strings.size());
        
        // BrtSSTItem records
        for (String str : strings) {
            writeBrtSSTItem(w, str);
        }
        
        // BrtEndSst
        w.writeEmptyRecord(Biff12RecordType.BrtEndSst);
        
        return w.toByteArray();
    }
    
    /**
     * 写入BrtSSTItem记录
     * 结构：RichStr = flags(1) + XLWideString
     */
    private void writeBrtSSTItem(Biff12Writer w, String str) throws IOException {
        int strLen = str.length();
        int recordSize = 1 + 4 + strLen * 2;  // flags(1) + 字符数(4) + UTF-16LE字符
        
        w.writeRecordHeader(Biff12RecordType.BrtSSTItem, recordSize);
        
        // RichStr flags (1 byte): fRichStr=0, fExtStr=0
        w.writeBytes(SST_FLAGS_ZERO);
        
        // XLWideString
        w.writeXLWideString(str);
    }
}