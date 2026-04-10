package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.io.OffHeapOutputStream;
import java.io.IOException;
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
    private final OffHeapOutputStream outputStream;
    private final long capacity;
    private long position = 0;
    
    public RecordWriter(long capacity) {
        this.allocator = AllocatorFactory.createDefaultAllocator();
        this.outputBlock = allocator.allocate(capacity);
        this.capacity = capacity;
        this.outputStream = null;
    }
    
    public RecordWriter(long capacity, OffHeapOutputStream outputStream) {
        this.allocator = AllocatorFactory.createDefaultAllocator();
        this.outputBlock = allocator.allocate(capacity);
        this.capacity = capacity;
        this.outputStream = Objects.requireNonNull(outputStream, "OutputStream must not be null");
    }
    
    private void ensureCapacity(int required) throws IOException {
        if (outputStream != null && position + required > capacity) {
            flush();
        }
    }
    
    public void writeRecordHeader(int recordType, int recordSize) throws IOException {
        ensureCapacity(8);
        outputBlock.putInt(position, recordType);
        position += 4;
        outputBlock.putInt(position, recordSize);
        position += 4;
    }
    
    public void writeByte(byte value) throws IOException {
        ensureCapacity(1);
        outputBlock.putByte(position, value);
        position += 1;
    }
    
    public void writeShort(short value) throws IOException {
        ensureCapacity(2);
        outputBlock.putShort(position, value);
        position += 2;
    }
    
    public void writeInt(int value) throws IOException {
        ensureCapacity(4);
        outputBlock.putInt(position, value);
        position += 4;
    }
    
    public void writeLong(long value) throws IOException {
        ensureCapacity(8);
        outputBlock.putLong(position, value);
        position += 8;
    }
    
    public void writeDouble(double value) throws IOException {
        ensureCapacity(8);
        outputBlock.putDouble(position, value);
        position += 8;
    }
    
    public void writeBytes(byte[] data) throws IOException {
        ensureCapacity(data.length);
        outputBlock.putBytes(position, data, 0, data.length);
        position += data.length;
    }
    
    public long getPosition() {
        return position;
    }
    
    public MemoryBlock getOutputBlock() {
        return outputBlock;
    }
    
    public void flush() throws IOException {
        if (outputStream != null && position > 0) {
            outputStream.writeBlock(outputBlock, position);
            position = 0;
        }
    }
    
    public void close() throws IOException {
        flush();
        outputBlock.close();
    }
}