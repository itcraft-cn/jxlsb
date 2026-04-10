package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * 未知记录类型
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class UnknownRecord extends BiffRecord {
    
    public UnknownRecord(int recordType, int recordSize, MemoryBlock dataBlock) {
        super(recordType, recordSize, dataBlock);
    }
    
    @Override
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(recordType, recordSize);
        if (dataBlock != null && recordSize > 0) {
            byte[] data = new byte[recordSize];
            dataBlock.getBytes(0, data, 0, recordSize);
            writer.writeBytes(data);
        }
    }
}