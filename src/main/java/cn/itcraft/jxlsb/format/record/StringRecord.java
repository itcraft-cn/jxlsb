package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.nio.charset.StandardCharsets;

/**
 * String记录
 * 
 * <p>BIFF12记录类型0x00F9，存储字符串数据。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class StringRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x00F9;
    
    private static final int LENGTH_OFFSET = 0;
    private static final int STRING_DATA_OFFSET = 4;
    
    public StringRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static StringRecord create(String text, MemoryBlock dataBlock) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int length = Math.min(bytes.length, 65535);
        
        dataBlock.putInt(LENGTH_OFFSET, length);
        dataBlock.putBytes(STRING_DATA_OFFSET, bytes, 0, length);
        
        return new StringRecord(4 + length, dataBlock);
    }
    
    public int getLength() {
        return dataBlock.getInt(LENGTH_OFFSET);
    }
    
    public String getString() {
        int length = getLength();
        byte[] bytes = new byte[length];
        dataBlock.getBytes(STRING_DATA_OFFSET, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeInt(getLength());
        byte[] data = new byte[getLength()];
        dataBlock.getBytes(STRING_DATA_OFFSET, data, 0, getLength());
        writer.writeBytes(data);
    }
}