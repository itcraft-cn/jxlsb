package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import java.util.Objects;

/**
 * XLSB记录写入器
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class RecordWriter {
    
    private final OffHeapAllocator allocator;
    private final MemoryBlock outputBlock;
    private long position = 0;
    
    public RecordWriter(long capacity) {
        this.allocator = AllocatorFactory.createDefaultAllocator();
        this.outputBlock = allocator.allocate(capacity);
    }
    
    public void writeRecordHeader(int recordType, int recordSize) {
        outputBlock.putInt(position, recordType);
        position += 4;
        outputBlock.putInt(position, recordSize);
        position += 4;
    }
    
    public void writeByte(byte value) {
        outputBlock.putByte(position, value);
        position += 1;
    }
    
    public void writeShort(short value) {
        outputBlock.putShort(position, value);
        position += 2;
    }
    
    public void writeInt(int value) {
        outputBlock.putInt(position, value);
        position += 4;
    }
    
    public void writeLong(long value) {
        outputBlock.putLong(position, value);
        position += 8;
    }
    
    public void writeDouble(double value) {
        outputBlock.putDouble(position, value);
        position += 8;
    }
    
    public void writeBytes(byte[] data) {
        outputBlock.putBytes(position, data, 0, data.length);
        position += data.length;
    }
    
    public long getPosition() {
        return position;
    }
    
    public MemoryBlock getOutputBlock() {
        return outputBlock;
    }
    
    public void close() {
        outputBlock.close();
    }
}