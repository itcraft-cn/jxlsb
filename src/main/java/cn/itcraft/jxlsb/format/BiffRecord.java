package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.io.IOException;
import java.util.Objects;

/**
 * BIFF12记录抽象基类
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public abstract class BiffRecord implements AutoCloseable {
    
    protected final MemoryBlock dataBlock;
    protected final int recordType;
    protected final int recordSize;
    
    protected BiffRecord(int recordType, int recordSize, MemoryBlock dataBlock) {
        this.recordType = recordType;
        this.recordSize = recordSize;
        this.dataBlock = dataBlock;
    }
    
    public int getRecordType() {
        return recordType;
    }
    
    public int getRecordSize() {
        return recordSize;
    }
    
    public MemoryBlock getDataBlock() {
        return dataBlock;
    }
    
    public abstract void writeTo(RecordWriter writer) throws IOException;
    
    @Override
    public void close() {
        if (dataBlock != null) {
            dataBlock.close();
        }
    }
}