package cn.itcraft.jxlsb.memory.impl;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.exception.MemoryAllocationException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java 8+堆外内存分配器实现
 * 
 * <p>使用ByteBuffer.allocateDirect()分配堆外内存，
 * 通过ByteBuffer API进行内存访问操作。
 * 
 * <p>兼容Java 8到最新版本，不依赖Unsafe内部API。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class ByteBufferAllocator implements OffHeapAllocator {
    
    private static final String STRATEGY_NAME = "ByteBuffer-Direct";
    private final AtomicLong totalAllocated = new AtomicLong(0);
    
    @Override
    public MemoryBlock allocate(long size) {
        if (size <= 0) {
            throw new MemoryAllocationException(size, "Size must be positive");
        }
        
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            totalAllocated.addAndGet(size);
            
            return new ByteBufferMemoryBlock(buffer, size, this);
        } catch (OutOfMemoryError e) {
            throw new MemoryAllocationException(size, "OutOfMemoryError: " + e.getMessage());
        }
    }
    
    @Override
    public MemoryBlock allocateFromPool(long size) {
        return allocate(size);
    }
    
    @Override
    public void deallocate(MemoryBlock block) {
        if (block instanceof ByteBufferMemoryBlock) {
            ByteBufferMemoryBlock byteBufferBlock = (ByteBufferMemoryBlock) block;
            totalAllocated.addAndGet(-byteBufferBlock.size());
            byteBufferBlock.forceClose();
        }
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public long getTotalAllocated() {
        return totalAllocated.get();
    }
}