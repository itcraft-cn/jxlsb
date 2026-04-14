package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.container.SheetInfo;
import cn.itcraft.jxlsb.format.VarIntReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class WorkbookReader implements AutoCloseable {
    
    private final InputStream inputStream;
    
    public WorkbookReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    public List<SheetInfo> parseSheetList() throws IOException {
        List<SheetInfo> sheets = new ArrayList<>();
        byte[] buffer = new byte[8192];
        int bytesRead;
        int offset = 0;
        
        while ((bytesRead = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += bytesRead;
            int processed = processWorkbookBuffer(buffer, offset, sheets);
            
            if (processed > 0 && processed < offset) {
                byte[] remaining = new byte[offset - processed];
                System.arraycopy(buffer, processed, remaining, 0, remaining.length);
                System.arraycopy(remaining, 0, buffer, 0, remaining.length);
                offset = remaining.length;
            } else {
                offset = 0;
            }
        }
        
        return sheets;
    }
    
    private int processWorkbookBuffer(byte[] buffer, int length, List<SheetInfo> sheets) {
        int pos = 0;
        
        while (pos + 2 <= length) {
            int recordType = VarIntReader.readVarInt(buffer, pos);
            int typeSize = VarIntReader.varIntSize(recordType);
            pos += typeSize;
            
            if (pos >= length) break;
            
            int recordSize = VarIntReader.readVarSize(buffer, pos);
            int sizeBytes = VarIntReader.varSizeSize(recordSize);
            pos += sizeBytes;
            
            if (recordSize > 0 && pos + recordSize > length) {
                return pos - typeSize - sizeBytes;
            }
            
            if (recordType == Biff12RecordType.BrtBundleSh) {
                SheetInfo info = parseBrtBundleSh(buffer, pos, recordSize);
                if (info != null) {
                    sheets.add(info);
                }
            }
            
            pos += recordSize;
        }
        
        return pos;
    }
    
    private SheetInfo parseBrtBundleSh(byte[] buffer, int offset, int size) {
        int pos = offset;
        int hsState = VarIntReader.readIntLE(buffer, pos);
        pos += 4;
        
        int iTabId = VarIntReader.readIntLE(buffer, pos);
        pos += 4;
        
        String relId = readXLWideString(buffer, pos);
        pos += 4 + relId.length() * 2;
        
        String name = readXLWideString(buffer, pos);
        
        return new SheetInfo(name, iTabId, relId);
    }
    
    private String readXLWideString(byte[] buffer, int offset) {
        int length = VarIntReader.readIntLE(buffer, offset);
        if (length == 0) return "";
        
        byte[] bytes = new byte[length * 2];
        System.arraycopy(buffer, offset + 4, bytes, 0, bytes.length);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_16LE);
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}