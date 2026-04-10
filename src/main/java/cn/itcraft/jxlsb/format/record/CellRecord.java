package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.data.CellType;

/**
 * Cell记录
 * 
 * <p>BIFF12记录类型0x0143，存储单元格数据。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class CellRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0143;
    
    private static final int ROW_INDEX_OFFSET = 0;
    private static final int COL_INDEX_OFFSET = 4;
    private static final int CELL_TYPE_OFFSET = 8;
    private static final int VALUE_OFFSET = 12;
    
    public CellRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static CellRecord create(int rowIndex, int colIndex, 
                                    CellType cellType, Object value,
                                    MemoryBlock dataBlock) {
        dataBlock.putInt(ROW_INDEX_OFFSET, rowIndex);
        dataBlock.putInt(COL_INDEX_OFFSET, colIndex);
        dataBlock.putInt(CELL_TYPE_OFFSET, cellType.getCode());
        
        switch (cellType) {
            case NUMBER:
                dataBlock.putDouble(VALUE_OFFSET, (Double) value);
                break;
            case DATE:
                dataBlock.putLong(VALUE_OFFSET, (Long) value);
                break;
            case BOOLEAN:
                dataBlock.putByte(VALUE_OFFSET, (Boolean) value ? (byte) 1 : (byte) 0);
                break;
            case TEXT:
                String text = (String) value;
                byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                dataBlock.putInt(VALUE_OFFSET, bytes.length);
                dataBlock.putBytes(VALUE_OFFSET + 4, bytes, 0, Math.min(bytes.length, 1000));
                break;
            default:
                break;
        }
        
        return new CellRecord(calculateSize(cellType, value), dataBlock);
    }
    
    private static int calculateSize(CellType cellType, Object value) {
        switch (cellType) {
            case NUMBER:
                return 20;
            case DATE:
                return 20;
            case BOOLEAN:
                return 13;
            case TEXT:
                String text = (String) value;
                return 12 + Math.min(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, 1000) + 4;
            default:
                return 12;
        }
    }
    
    public int getRowIndex() {
        return dataBlock.getInt(ROW_INDEX_OFFSET);
    }
    
    public int getColIndex() {
        return dataBlock.getInt(COL_INDEX_OFFSET);
    }
    
    public CellType getCellType() {
        int code = dataBlock.getInt(CELL_TYPE_OFFSET);
        return CellType.fromCode(code);
    }
    
    public double getNumberValue() {
        return dataBlock.getDouble(VALUE_OFFSET);
    }
    
    public long getDateValue() {
        return dataBlock.getLong(VALUE_OFFSET);
    }
    
    public boolean getBooleanValue() {
        return dataBlock.getByte(VALUE_OFFSET) != 0;
    }
    
    public String getTextValue() {
        int length = dataBlock.getInt(VALUE_OFFSET);
        byte[] bytes = new byte[length];
        dataBlock.getBytes(VALUE_OFFSET + 4, bytes, 0, length);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        byte[] data = new byte[recordSize];
        dataBlock.getBytes(0, data, 0, recordSize);
        writer.writeBytes(data);
    }
}