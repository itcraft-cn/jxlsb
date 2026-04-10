package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * MergeCell记录
 * 
 * <p>BIFF12记录类型0x00B7，存储合并单元格信息。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class MergeCellRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x00B7;
    
    private static final int FIRST_ROW_OFFSET = 0;
    private static final int LAST_ROW_OFFSET = 2;
    private static final int FIRST_COL_OFFSET = 4;
    private static final int LAST_COL_OFFSET = 6;
    
    public MergeCellRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static MergeCellRecord create(int firstRow, int lastRow,
                                         int firstCol, int lastCol,
                                         MemoryBlock dataBlock) {
        dataBlock.putShort(FIRST_ROW_OFFSET, (short) firstRow);
        dataBlock.putShort(LAST_ROW_OFFSET, (short) lastRow);
        dataBlock.putShort(FIRST_COL_OFFSET, (short) firstCol);
        dataBlock.putShort(LAST_COL_OFFSET, (short) lastCol);
        
        return new MergeCellRecord(8, dataBlock);
    }
    
    public int getFirstRow() {
        return dataBlock.getShort(FIRST_ROW_OFFSET) & 0xFFFF;
    }
    
    public int getLastRow() {
        return dataBlock.getShort(LAST_ROW_OFFSET) & 0xFFFF;
    }
    
    public int getFirstCol() {
        return dataBlock.getShort(FIRST_COL_OFFSET) & 0xFFFF;
    }
    
    public int getLastCol() {
        return dataBlock.getShort(LAST_COL_OFFSET) & 0xFFFF;
    }
    
    @Override
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeShort((short) getFirstRow());
        writer.writeShort((short) getLastRow());
        writer.writeShort((short) getFirstCol());
        writer.writeShort((short) getLastCol());
    }
}