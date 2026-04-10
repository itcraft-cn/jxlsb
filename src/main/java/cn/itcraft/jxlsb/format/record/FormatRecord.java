package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.nio.charset.StandardCharsets;

/**
 * Format记录
 * 
 * <p>BIFF12记录类型0x0041，存储单元格格式字符串。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class FormatRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0041;
    
    private static final int FORMAT_INDEX_OFFSET = 0;
    private static final int FORMAT_LENGTH_OFFSET = 4;
    private static final int FORMAT_STRING_OFFSET = 8;
    
    public static final int FORMAT_GENERAL = 0;
    public static final int FORMAT_NUMBER = 1;
    public static final int FORMAT_DECIMAL = 2;
    public static final int FORMAT_CURRENCY = 3;
    public static final int FORMAT_DATE = 14;
    public static final int FORMAT_TIME = 20;
    public static final int FORMAT_DATETIME = 22;
    
    public FormatRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static FormatRecord create(int formatIndex, String formatString, 
                                      MemoryBlock dataBlock) {
        dataBlock.putInt(FORMAT_INDEX_OFFSET, formatIndex);
        
        byte[] bytes = formatString.getBytes(StandardCharsets.UTF_16LE);
        int length = Math.min(bytes.length, 65535);
        
        dataBlock.putInt(FORMAT_LENGTH_OFFSET, length / 2);
        dataBlock.putBytes(FORMAT_STRING_OFFSET, bytes, 0, length);
        
        return new FormatRecord(8 + length, dataBlock);
    }
    
    public int getFormatIndex() {
        return dataBlock.getInt(FORMAT_INDEX_OFFSET);
    }
    
    public int getFormatLength() {
        return dataBlock.getInt(FORMAT_LENGTH_OFFSET);
    }
    
    public String getFormatString() {
        int length = getFormatLength() * 2;
        byte[] bytes = new byte[length];
        dataBlock.getBytes(FORMAT_STRING_OFFSET, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeInt(getFormatIndex());
        writer.writeInt(getFormatLength());
        
        int length = getFormatLength() * 2;
        byte[] bytes = new byte[length];
        dataBlock.getBytes(FORMAT_STRING_OFFSET, bytes, 0, length);
        writer.writeBytes(bytes);
    }
}