package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BeginSheet记录
 * 
 * <p>BIFF12记录类型0x0085，标记Sheet开始。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BeginSheetRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0085;
    
    private static final int SHEET_INDEX_OFFSET = 0;
    
    public BeginSheetRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static BeginSheetRecord create(int sheetIndex, MemoryBlock dataBlock) {
        dataBlock.putInt(SHEET_INDEX_OFFSET, sheetIndex);
        return new BeginSheetRecord(4, dataBlock);
    }
    
    public int getSheetIndex() {
        return dataBlock.getInt(SHEET_INDEX_OFFSET);
    }
    
    @Override
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeInt(getSheetIndex());
    }
}