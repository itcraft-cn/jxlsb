package cn.itcraft.jxlsb.memory;

/**
 * Mock内存块实现（用于测试）
 */
final class MockMemoryBlock implements MemoryBlock {
    
    private final byte[] data;
    private final long size;
    private volatile boolean closed = false;
    
    MockMemoryBlock(long size) {
        this.size = size;
        this.data = new byte[(int) size];
    }
    
    @Override
    public void putByte(long offset, byte value) {
        checkNotClosed();
        data[(int) offset] = value;
    }
    
    @Override
    public void putShort(long offset, short value) {
        checkNotClosed();
        data[(int) offset] = (byte) (value & 0xFF);
        data[(int) offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
    
    @Override
    public void putInt(long offset, int value) {
        checkNotClosed();
        data[(int) offset] = (byte) (value & 0xFF);
        data[(int) offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[(int) offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[(int) offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
    
    @Override
    public void putLong(long offset, long value) {
        checkNotClosed();
        for (int i = 0; i < 8; i++) {
            data[(int) offset + i] = (byte) ((value >> (i * 8)) & 0xFF);
        }
    }
    
    @Override
    public void putDouble(long offset, double value) {
        checkNotClosed();
        putLong(offset, Double.doubleToLongBits(value));
    }
    
    @Override
    public void putBytes(long offset, byte[] src, int srcOffset, int length) {
        checkNotClosed();
        System.arraycopy(src, srcOffset, data, (int) offset, length);
    }
    
    @Override
    public byte getByte(long offset) {
        checkNotClosed();
        return data[(int) offset];
    }
    
    @Override
    public short getShort(long offset) {
        checkNotClosed();
        return (short) (data[(int) offset] | (data[(int) offset + 1] << 8));
    }
    
    @Override
    public int getInt(long offset) {
        checkNotClosed();
        return data[(int) offset] |
               (data[(int) offset + 1] << 8) |
               (data[(int) offset + 2] << 16) |
               (data[(int) offset + 3] << 24);
    }
    
    @Override
    public long getLong(long offset) {
        checkNotClosed();
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (data[(int) offset + i] & 0xFF)) << (i * 8);
        }
        return value;
    }
    
    @Override
    public double getDouble(long offset) {
        checkNotClosed();
        return Double.longBitsToDouble(getLong(offset));
    }
    
    @Override
    public void getBytes(long offset, byte[] dst, int dstOffset, int length) {
        checkNotClosed();
        System.arraycopy(data, (int) offset, dst, dstOffset, length);
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
    
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Memory block already closed");
        }
    }
}