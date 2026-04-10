package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BeginRow记录
 * 
 * <p>BIFF12记录类型0x0087，标记Row开始。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BeginRowRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0087;
    
    private static final int ROW_INDEX_OFFSET = 0;
    private static final int COLUMN_COUNT_OFFSET = 4;
    
    public BeginRowRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static BeginRowRecord create(int rowIndex, int columnCount, 
                                        MemoryBlock dataBlock) {
        dataBlock.putInt(ROW_INDEX_OFFSET, rowIndex);
        dataBlock.putInt(COLUMN_COUNT_OFFSET, columnCount);
        return new BeginRowRecord(8, dataBlock);
    }
    
    public int getRowIndex() {
        return dataBlock.getInt(ROW_INDEX_OFFSET);
    }
    
    public int getColumnCount() {
        return dataBlock.getInt(COLUMN_COUNT_OFFSET);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeInt(getRowIndex());
        writer.writeInt(getColumnCount());
    }
}