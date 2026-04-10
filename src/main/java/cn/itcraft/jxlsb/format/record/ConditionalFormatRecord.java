package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.nio.charset.StandardCharsets;

/**
 * ConditionalFormat记录
 * 
 * <p>BIFF12记录类型0x01CD，存储条件格式规则。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class ConditionalFormatRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x01CD;
    
    private static final int TYPE_OFFSET = 0;
    private static final int OPERATOR_OFFSET = 1;
    private static final int FORMULA1_LENGTH_OFFSET = 2;
    private static final int FORMULA1_DATA_OFFSET = 6;
    
    public static final byte TYPE_CELL_IS = 0;
    public static final byte TYPE_EXPRESSION = 1;
    public static final byte TYPE_COLOR_SCALE = 2;
    public static final byte TYPE_DATA_BAR = 3;
    public static final byte TYPE_ICON_SET = 4;
    
    public static final byte OPERATOR_NO_COMPARISON = 0;
    public static final byte OPERATOR_BETWEEN = 1;
    public static final byte OPERATOR_NOT_BETWEEN = 2;
    public static final byte OPERATOR_EQUAL = 3;
    public static final byte OPERATOR_NOT_EQUAL = 4;
    public static final byte OPERATOR_GREATER_THAN = 5;
    public static final byte OPERATOR_LESS_THAN = 6;
    public static final byte OPERATOR_GREATER_EQUAL = 7;
    public static final byte OPERATOR_LESS_EQUAL = 8;
    
    public ConditionalFormatRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static ConditionalFormatRecord create(byte type, byte operator,
                                                 String formula1, MemoryBlock dataBlock) {
        dataBlock.putByte(TYPE_OFFSET, type);
        dataBlock.putByte(OPERATOR_OFFSET, operator);
        
        byte[] formulaBytes = formula1.getBytes(StandardCharsets.UTF_8);
        int length = Math.min(formulaBytes.length, 65535);
        
        dataBlock.putInt(FORMULA1_LENGTH_OFFSET, length);
        dataBlock.putBytes(FORMULA1_DATA_OFFSET, formulaBytes, 0, length);
        
        return new ConditionalFormatRecord(6 + length, dataBlock);
    }
    
    public byte getType() {
        return dataBlock.getByte(TYPE_OFFSET);
    }
    
    public byte getOperator() {
        return dataBlock.getByte(OPERATOR_OFFSET);
    }
    
    public int getFormula1Length() {
        return dataBlock.getInt(FORMULA1_LENGTH_OFFSET);
    }
    
    public String getFormula1() {
        int length = getFormula1Length();
        byte[] bytes = new byte[length];
        dataBlock.getBytes(FORMULA1_DATA_OFFSET, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeByte(getType());
        writer.writeByte(getOperator());
        writer.writeInt(getFormula1Length());
        
        byte[] data = new byte[getFormula1Length()];
        dataBlock.getBytes(FORMULA1_DATA_OFFSET, data, 0, getFormula1Length());
        writer.writeBytes(data);
    }
}