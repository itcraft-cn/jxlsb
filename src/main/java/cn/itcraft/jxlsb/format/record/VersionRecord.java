package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * Version记录
 * 
 * <p>BIFF12记录类型0x0080，存储版本信息。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class VersionRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0080;
    
    private static final int VERSION_OFFSET = 0;
    
    public static final int VERSION_2007 = 0x0600;
    public static final int VERSION_2010 = 0x0700;
    public static final int VERSION_2013 = 0x0800;
    public static final int VERSION_2016 = 0x0900;
    
    public VersionRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static VersionRecord create(int version, MemoryBlock dataBlock) {
        dataBlock.putInt(VERSION_OFFSET, version);
        return new VersionRecord(4, dataBlock);
    }
    
    public int getVersion() {
        return dataBlock.getInt(VERSION_OFFSET);
    }
    
    @Override
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        writer.writeInt(getVersion());
    }
}