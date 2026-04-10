package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * EndRow记录
 * 
 * <p>BIFF12记录类型0x0088，标记Row结束。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class EndRowRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0088;
    
    public EndRowRecord() {
        super(RECORD_TYPE, 0, null);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, 0);
    }
}