package cn.itcraft.jxlsb.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 共享字符串表（SST）
 * 严格按照MS-XLSB规范实现
 */
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
    
    /**
     * 生成sharedStrings.bin内容
     * 记录序列：BrtBeginSst -> BrtSSTItem* -> BrtEndSst
     */
    public byte[] toBiff12Bytes() throws IOException {
        Biff12Writer w = new Biff12Writer();
        
        // BrtBeginSst
        // 结构：cTotals(4) + cUnique(4)
        w.writeRecordHeader(Biff12RecordType.BrtBeginSst, 8);
        w.writeIntLE(totalCount);
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
     * 结构：string(XLWideString)
     */
    private void writeBrtSSTItem(Biff12Writer w, String str) throws IOException {
        // 计算大小
        int strLen = str.length();
        int recordSize = 4 + strLen * 2;  // 字符数(4) + UTF-16LE字符
        
        w.writeRecordHeader(Biff12RecordType.BrtSSTItem, recordSize);
        w.writeXLWideString(str);
    }
}