package cn.itcraft.jxlsb.memory.impl;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Java 23+内存块实现
 * 
 * <p>基于MemorySegment的堆外内存块，使用Foreign Memory API访问。
 * 使用小端序（Little-Endian）存储数据。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
final class MemorySegmentBlock implements MemoryBlock {
    
    private final MemorySegment segment;
    private final long size;
    private final MemorySegmentAllocator allocator;
    private volatile boolean closed = false;
    
    MemorySegmentBlock(MemorySegment segment, long size, MemorySegmentAllocator allocator) {
        this.segment = segment;
        this.size = size;
        this.allocator = allocator;
    }
    
    @Override
    public void putByte(long offset, byte value) {
        checkNotClosed();
        checkBounds(offset, 1);
        segment.set(ValueLayout.JAVA_BYTE, offset, value);
    }
    
    @Override
    public void putShort(long offset, short value) {
        checkNotClosed();
        checkBounds(offset, 2);
        segment.set(ValueLayout.JAVA_SHORT_UNALIGNED, offset, value);
    }
    
    @Override
    public void putInt(long offset, int value) {
        checkNotClosed();
        checkBounds(offset, 4);
        segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, value);
    }
    
    @Override
    public void putLong(long offset, long value) {
        checkNotClosed();
        checkBounds(offset, 8);
        segment.set(ValueLayout.JAVA_LONG_UNALIGNED, offset, value);
    }
    
    @Override
    public void putDouble(long offset, double value) {
        checkNotClosed();
        checkBounds(offset, 8);
        segment.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset, value);
    }
    
    @Override
    public void putBytes(long offset, byte[] src, int srcOffset, int length) {
        checkNotClosed();
        checkBounds(offset, length);
        MemorySegment.copy(src, srcOffset, segment, ValueLayout.JAVA_BYTE, offset, length);
    }
    
    @Override
    public byte getByte(long offset) {
        checkNotClosed();
        checkBounds(offset, 1);
        return segment.get(ValueLayout.JAVA_BYTE, offset);
    }
    
    @Override
    public short getShort(long offset) {
        checkNotClosed();
        checkBounds(offset, 2);
        return segment.get(ValueLayout.JAVA_SHORT_UNALIGNED, offset);
    }
    
    @Override
    public int getInt(long offset) {
        checkNotClosed();
        checkBounds(offset, 4);
        return segment.get(ValueLayout.JAVA_INT_UNALIGNED, offset);
    }
    
    @Override
    public long getLong(long offset) {
        checkNotClosed();
        checkBounds(offset, 8);
        return segment.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
    }
    
    @Override
    public double getDouble(long offset) {
        checkNotClosed();
        checkBounds(offset, 8);
        return segment.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, offset);
    }
    
    @Override
    public void getBytes(long offset, byte[] dst, int dstOffset, int length) {
        checkNotClosed();
        checkBounds(offset, length);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, dst, dstOffset, length);
    }
    
    @Override
    public long size() {
        return size;
    }
    
    @Override
    public long getAddress() {
        return segment.address();
    }
    
    @Override
    public void close() {
        closed = true;
    }
    
    void forceClose() {
        closed = true;
    }
    
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Memory block already closed");
        }
    }
    
    private void checkBounds(long offset, int dataSize) {
        if (offset < 0 || offset + dataSize > size) {
            throw new IndexOutOfBoundsException(
                String.format("Offset %d + size %d out of bounds for block size %d", 
                             offset, dataSize, size));
        }
    }
}