package cn.itcraft.jxlsb.memory.impl;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.exception.MemoryAllocationException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java 23+堆外内存分配器实现
 * 
 * <p>使用Foreign Memory API（MemorySegment）管理堆外内存，
 * 更安全、更现代化，性能与Unsafe相当。
 * 
 * <p>需要Java 22+（Foreign Memory API正式版）。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class MemorySegmentAllocator implements OffHeapAllocator {
    
    private static final String STRATEGY_NAME = "MemorySegment-ForeignAPI";
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final Arena globalArena;
    
    public MemorySegmentAllocator() {
        this.globalArena = Arena.ofShared();
    }
    
    @Override
    public MemoryBlock allocate(long size) {
        if (size <= 0) {
            throw new MemoryAllocationException(size, "Size must be positive");
        }
        
        try {
            MemorySegment segment = globalArena.allocate(size);
            totalAllocated.addAndGet(size);
            
            return new MemorySegmentBlock(segment, size, this);
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
        if (block instanceof MemorySegmentBlock) {
            MemorySegmentBlock segmentBlock = (MemorySegmentBlock) block;
            totalAllocated.addAndGet(-segmentBlock.size());
            segmentBlock.forceClose();
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