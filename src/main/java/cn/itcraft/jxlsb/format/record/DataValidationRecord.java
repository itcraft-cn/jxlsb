package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.nio.charset.StandardCharsets;

/**
 * DataValidation记录
 * 
 * <p>BIFF12记录类型0x01B2，存储数据验证规则。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class DataValidationRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x01B2;
    
    private static final int TYPE_OFFSET = 0;
    private static final int ERROR_STYLE_OFFSET = 1;
    private static final int ALLOW_BLANK_OFFSET = 2;
    private static final int FORMULA1_LENGTH_OFFSET = 4;
    private static final int FORMULA1_DATA_OFFSET = 8;
    
    public static final byte TYPE_ANY = 0;
    public static final byte TYPE_WHOLE = 1;
    public static final byte TYPE_DECIMAL = 2;
    public static final byte TYPE_LIST = 3;
    public static final byte TYPE_DATE = 4;
    public static final byte TYPE_TIME = 5;
    public static final byte TYPE_TEXT_LENGTH = 6;
    public static final byte TYPE_CUSTOM = 7;
    
    public static final byte ERROR_STYLE_STOP = 0;
    public static final byte ERROR_STYLE_WARNING = 1;
    public static final byte ERROR_STYLE_INFORMATION = 2;
    
    public DataValidationRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static DataValidationRecord create(byte type, byte errorStyle,
                                              boolean allowBlank, String formula1,
                                              MemoryBlock dataBlock) {
        dataBlock.putByte(TYPE_OFFSET, type);
        dataBlock.putByte(ERROR_STYLE_OFFSET, errorStyle);
        dataBlock.putByte(ALLOW_BLANK_OFFSET, (byte) (allowBlank ? 1 : 0));
        
        byte[] formulaBytes = formula1.getBytes(StandardCharsets.UTF_8);
        int length = Math.min(formulaBytes.length, 65535);
        
        dataBlock.putInt(FORMULA1_LENGTH_OFFSET, length);
        dataBlock.putBytes(FORMULA1_DATA_OFFSET, formulaBytes, 0, length);
        
        return new DataValidationRecord(8 + length, dataBlock);
    }
    
    public byte getType() {
        return dataBlock.getByte(TYPE_OFFSET);
    }
    
    public byte getErrorStyle() {
        return dataBlock.getByte(ERROR_STYLE_OFFSET);
    }
    
    public boolean isAllowBlank() {
        return dataBlock.getByte(ALLOW_BLANK_OFFSET) != 0;
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
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeByte(getType());
        writer.writeByte(getErrorStyle());
        writer.writeByte(isAllowBlank() ? (byte) 1 : (byte) 0);
        writer.writeInt(getFormula1Length());
        
        byte[] data = new byte[getFormula1Length()];
        dataBlock.getBytes(FORMULA1_DATA_OFFSET, data, 0, getFormula1Length());
        writer.writeBytes(data);
    }
}