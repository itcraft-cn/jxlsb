package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * Index记录
 * 
 * <p>BIFF12记录类型0x0089，存储行索引信息，优化行访问性能。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class IndexRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0089;
    
    private static final int FIRST_ROW_OFFSET = 0;
    private static final int LAST_ROW_OFFSET = 4;
    private static final int FIRST_COLUMN_OFFSET = 8;
    private static final int LAST_COLUMN_OFFSET = 12;
    
    public IndexRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static IndexRecord create(int firstRow, int lastRow, 
                                     int firstCol, int lastCol,
                                     MemoryBlock dataBlock) {
        dataBlock.putInt(FIRST_ROW_OFFSET, firstRow);
        dataBlock.putInt(LAST_ROW_OFFSET, lastRow);
        dataBlock.putInt(FIRST_COLUMN_OFFSET, firstCol);
        dataBlock.putInt(LAST_COLUMN_OFFSET, lastCol);
        
        return new IndexRecord(16, dataBlock);
    }
    
    public int getFirstRow() {
        return dataBlock.getInt(FIRST_ROW_OFFSET);
    }
    
    public int getLastRow() {
        return dataBlock.getInt(LAST_ROW_OFFSET);
    }
    
    public int getFirstColumn() {
        return dataBlock.getInt(FIRST_COLUMN_OFFSET);
    }
    
    public int getLastColumn() {
        return dataBlock.getInt(LAST_COLUMN_OFFSET);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeInt(getFirstRow());
        writer.writeInt(getLastRow());
        writer.writeInt(getFirstColumn());
        writer.writeInt(getLastColumn());
    }
}