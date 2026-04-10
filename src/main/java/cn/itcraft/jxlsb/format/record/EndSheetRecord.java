package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.AllocatorFactory;

/**
 * EndSheet记录
 * 
 * <p>BIFF12记录类型0x0086，标记Sheet结束。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class EndSheetRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0086;
    
    public EndSheetRecord() {
        super(RECORD_TYPE, 0, null);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, 0);
    }
}