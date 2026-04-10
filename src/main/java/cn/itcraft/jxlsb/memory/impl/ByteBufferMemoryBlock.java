package cn.itcraft.jxlsb.memory.impl;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.nio.ByteBuffer;

/**
 * Java 8+内存块实现
 * 
 * <p>基于ByteBuffer的堆外内存块，使用ByteBuffer API访问。
 * 使用小端序（Little-Endian）存储数据。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
final class ByteBufferMemoryBlock implements MemoryBlock {
    
    private final ByteBuffer buffer;
    private final long size;
    private volatile boolean closed = false;
    
    ByteBufferMemoryBlock(ByteBuffer buffer, long size, ByteBufferAllocator allocator) {
        this.buffer = buffer;
        this.size = size;
    }
    
    @Override
    public void putByte(long offset, byte value) {
        checkNotClosed();
        checkBounds(offset, 1);
        buffer.put((int) offset, value);
    }
    
    @Override
    public void putShort(long offset, short value) {
        checkNotClosed();
        checkBounds(offset, 2);
        buffer.putShort((int) offset, value);
    }
    
    @Override
    public void putInt(long offset, int value) {
        checkNotClosed();
        checkBounds(offset, 4);
        buffer.putInt((int) offset, value);
    }
    
    @Override
    public void putLong(long offset, long value) {
        checkNotClosed();
        checkBounds(offset, 8);
        buffer.putLong((int) offset, value);
    }
    
    @Override
    public void putDouble(long offset, double value) {
        checkNotClosed();
        checkBounds(offset, 8);
        buffer.putDouble((int) offset, value);
    }
    
    @Override
    public void putBytes(long offset, byte[] src, int srcOffset, int length) {
        checkNotClosed();
        checkBounds(offset, length);
        buffer.position((int) offset);
        buffer.put(src, srcOffset, length);
    }
    
    @Override
    public byte getByte(long offset) {
        checkNotClosed();
        checkBounds(offset, 1);
        return buffer.get((int) offset);
    }
    
    @Override
    public short getShort(long offset) {
        checkNotClosed();
        checkBounds(offset, 2);
        return buffer.getShort((int) offset);
    }
    
    @Override
    public int getInt(long offset) {
        checkNotClosed();
        checkBounds(offset, 4);
        return buffer.getInt((int) offset);
    }
    
    @Override
    public long getLong(long offset) {
        checkNotClosed();
        checkBounds(offset, 8);
        return buffer.getLong((int) offset);
    }
    
    @Override
    public double getDouble(long offset) {
        checkNotClosed();
        checkBounds(offset, 8);
        return buffer.getDouble((int) offset);
    }
    
    @Override
    public void getBytes(long offset, byte[] dst, int dstOffset, int length) {
        checkNotClosed();
        checkBounds(offset, length);
        buffer.position((int) offset);
        buffer.get(dst, dstOffset, length);
    }
    
    @Override
    public long size() {
        return size;
    }
    
    @Override
    public long getAddress() {
        return 0;
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