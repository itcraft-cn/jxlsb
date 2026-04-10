package cn.itcraft.jxlsb.format;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class SharedStringsTable {
    
    private final List<String> strings = new ArrayList<>();
    private final Map<String, Integer> indexMap = new HashMap<>();
    private int totalCount = 0;
    
    public synchronized int addString(String str) {
        totalCount++;
        Integer idx = indexMap.get(str);
        if (idx != null) {
            return idx;
        }
        int newIndex = strings.size();
        strings.add(str);
        indexMap.put(str, newIndex);
        return newIndex;
    }
    
    public int getCount() {
        return strings.size();
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public String getString(int index) {
        return strings.get(index);
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // BEGIN_SST record
        writeRecordHeader(dos, Biff12Constants.BEGIN_SST, 8);
        writeIntLE(dos, strings.size());
        writeIntLE(dos, totalCount);
        
        // SST items
        for (String str : strings) {
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_16LE);
            writeRecordHeader(dos, Biff12Constants.SST_ITEM, 4 + strBytes.length);
            writeIntLE(dos, strBytes.length / 2); // char count
            dos.write(strBytes);
        }
        
        // END_SST record
        writeRecordHeader(dos, Biff12Constants.END_SST, 0);
        
        dos.flush();
        return baos.toByteArray();
    }
    
    private void writeRecordHeader(DataOutputStream dos, int type, int size) throws IOException {
        dos.write(type & 0xFF);
        dos.write((type >> 8) & 0xFF);
        dos.write(size & 0xFF);
        dos.write((size >> 8) & 0xFF);
    }
    
    private void writeIntLE(DataOutputStream dos, int value) throws IOException {
        dos.write(value & 0xFF);
        dos.write((value >> 8) & 0xFF);
        dos.write((value >> 16) & 0xFF);
        dos.write((value >> 24) & 0xFF);
    }
}