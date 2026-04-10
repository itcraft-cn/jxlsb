package cn.itcraft.jxlsb.memory;

/**
 * 内存池包装的内存块
 * 
 * <p>包装真实内存块，在close时归还到内存池而非直接释放。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
final class PooledMemoryBlock implements MemoryBlock {
    
    private final MemoryBlock delegate;
    private final long classSize;
    private final MemoryPool pool;
    private volatile boolean closed = false;
    
    PooledMemoryBlock(MemoryBlock delegate, long classSize, MemoryPool pool) {
        this.delegate = delegate;
        this.classSize = classSize;
        this.pool = pool;
    }
    
    @Override
    public void putByte(long offset, byte value) {
        delegate.putByte(offset, value);
    }
    
    @Override
    public void putShort(long offset, short value) {
        delegate.putShort(offset, value);
    }
    
    @Override
    public void putInt(long offset, int value) {
        delegate.putInt(offset, value);
    }
    
    @Override
    public void putLong(long offset, long value) {
        delegate.putLong(offset, value);
    }
    
    @Override
    public void putDouble(long offset, double value) {
        delegate.putDouble(offset, value);
    }
    
    @Override
    public void putBytes(long offset, byte[] src, int srcOffset, int length) {
        delegate.putBytes(offset, src, srcOffset, length);
    }
    
    @Override
    public byte getByte(long offset) {
        return delegate.getByte(offset);
    }
    
    @Override
    public short getShort(long offset) {
        return delegate.getShort(offset);
    }
    
    @Override
    public int getInt(long offset) {
        return delegate.getInt(offset);
    }
    
    @Override
    public long getLong(long offset) {
        return delegate.getLong(offset);
    }
    
    @Override
    public double getDouble(long offset) {
        return delegate.getDouble(offset);
    }
    
    @Override
    public void getBytes(long offset, byte[] dst, int dstOffset, int length) {
        delegate.getBytes(offset, dst, dstOffset, length);
    }
    
    @Override
    public long size() {
        return delegate.size();
    }
    
    @Override
    public long getAddress() {
        return delegate.getAddress();
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            pool.release(delegate, classSize);
        }
    }
}