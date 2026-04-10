package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbookWriterTest {
    
    @Test
    void writesWorkbookWithOneSheet() throws Exception {
        WorkbookWriter writer = new WorkbookWriter();
        writer.addSheet("Sheet1");
        
        byte[] data = writer.toBiff12Bytes();
        assertTrue(data.length > 0);
        
        // 使用变长编码解析记录类型
        int type = readVarInt(data, 0);
        assertEquals(Biff12RecordType.BrtBeginBook, type);
    }
    
    @Test
    void writesCorrectRecordSequence() throws Exception {
        WorkbookWriter writer = new WorkbookWriter();
        writer.addSheet("Test");
        
        byte[] data = writer.toBiff12Bytes();
        
        // 解析前几条记录
        int pos = 0;
        
        // BrtBeginBook
        int type = readVarInt(data, pos);
        pos += varIntSize(type);
        assertEquals(Biff12RecordType.BrtBeginBook, type);
        int size = readVarSize(data, pos);
        pos += varSizeSize(size);
        assertEquals(0, size);
        
        // BrtBeginBundleShs
        type = readVarInt(data, pos);
        pos += varIntSize(type);
        assertEquals(Biff12RecordType.BrtBeginBundleShs, type);
        size = readVarSize(data, pos);
        pos += varSizeSize(size);
        assertEquals(0, size);
    }
    
    private int readVarInt(byte[] data, int pos) {
        int b = data[pos] & 0xFF;
        if ((b & 0x80) == 0) return b;
        return (b & 0x7F) | ((data[pos+1] & 0x7F) << 7);
    }
    
    private int varIntSize(int value) {
        return value >= 128 ? 2 : 1;
    }
    
    private int readVarSize(byte[] data, int pos) {
        int b = data[pos] & 0xFF;
        if ((b & 0x80) == 0) return b;
        int result = b & 0x7F;
        b = data[pos+1] & 0xFF;
        result |= (b & 0x7F) << 7;
        if ((b & 0x80) == 0) return result;
        b = data[pos+2] & 0xFF;
        result |= (b & 0x7F) << 14;
        if ((b & 0x80) == 0) return result;
        b = data[pos+3] & 0xFF;
        result |= (b & 0x7F) << 21;
        return result;
    }
    
    private int varSizeSize(int value) {
        if (value < 128) return 1;
        if (value < 16384) return 2;
        if (value < 2097152) return 3;
        return 4;
    }
}