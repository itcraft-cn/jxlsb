package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * XFRecord（扩展格式记录）
 * 
 * <p>BIFF12记录类型0x0043，存储单元格扩展格式信息。
 * 包含字体、边框、填充、对齐等格式属性。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XFRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0043;
    
    private static final int FONT_INDEX_OFFSET = 0;
    private static final int FORMAT_INDEX_OFFSET = 2;
    private static final int CELL_PROTECTION_OFFSET = 4;
    private static final int HORIZONTAL_ALIGNMENT_OFFSET = 5;
    private static final int VERTICAL_ALIGNMENT_OFFSET = 6;
    private static final int TEXT_ROTATION_OFFSET = 7;
    private static final int FILL_INDEX_OFFSET = 8;
    private static final int BORDER_INDEX_OFFSET = 10;
    
    public static final byte ALIGN_GENERAL = 0;
    public static final byte ALIGN_LEFT = 1;
    public static final byte ALIGN_CENTER = 2;
    public static final byte ALIGN_RIGHT = 3;
    public static final byte ALIGN_FILL = 4;
    public static final byte ALIGN_JUSTIFY = 5;
    
    public static final byte VERTICAL_TOP = 0;
    public static final byte VERTICAL_CENTER = 1;
    public static final byte VERTICAL_BOTTOM = 2;
    public static final byte VERTICAL_JUSTIFY = 3;
    
    public XFRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static XFRecord create(int fontIndex, int formatIndex, 
                                  byte horizontalAlign, byte verticalAlign,
                                  int fillIndex, int borderIndex,
                                  MemoryBlock dataBlock) {
        dataBlock.putShort(FONT_INDEX_OFFSET, (short) fontIndex);
        dataBlock.putShort(FORMAT_INDEX_OFFSET, (short) formatIndex);
        dataBlock.putByte(CELL_PROTECTION_OFFSET, (byte) 0);
        dataBlock.putByte(HORIZONTAL_ALIGNMENT_OFFSET, horizontalAlign);
        dataBlock.putByte(VERTICAL_ALIGNMENT_OFFSET, verticalAlign);
        dataBlock.putByte(TEXT_ROTATION_OFFSET, (byte) 0);
        dataBlock.putShort(FILL_INDEX_OFFSET, (short) fillIndex);
        dataBlock.putShort(BORDER_INDEX_OFFSET, (short) borderIndex);
        
        return new XFRecord(12, dataBlock);
    }
    
    public int getFontIndex() {
        return dataBlock.getShort(FONT_INDEX_OFFSET) & 0xFFFF;
    }
    
    public int getFormatIndex() {
        return dataBlock.getShort(FORMAT_INDEX_OFFSET) & 0xFFFF;
    }
    
    public byte getHorizontalAlignment() {
        return dataBlock.getByte(HORIZONTAL_ALIGNMENT_OFFSET);
    }
    
    public byte getVerticalAlignment() {
        return dataBlock.getByte(VERTICAL_ALIGNMENT_OFFSET);
    }
    
    public int getFillIndex() {
        return dataBlock.getShort(FILL_INDEX_OFFSET) & 0xFFFF;
    }
    
    public int getBorderIndex() {
        return dataBlock.getShort(BORDER_INDEX_OFFSET) & 0xFFFF;
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        
        byte[] data = new byte[recordSize];
        dataBlock.getBytes(0, data, 0, recordSize);
        writer.writeBytes(data);
    }
}