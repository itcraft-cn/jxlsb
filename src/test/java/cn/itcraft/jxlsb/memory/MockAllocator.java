package cn.itcraft.jxlsb.memory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock内存分配器（用于测试）
 */
final class MockAllocator implements OffHeapAllocator {
    
    private final AtomicLong totalAllocated = new AtomicLong(0);
    
    @Override
    public MemoryBlock allocate(long size) {
        totalAllocated.addAndGet(size);
        return new MockMemoryBlock(size);
    }
    
    @Override
    public MemoryBlock allocateFromPool(long size) {
        return allocate(size);
    }
    
    @Override
    public void deallocate(MemoryBlock block) {
        totalAllocated.addAndGet(-block.size());
        block.close();
    }
    
    @Override
    public String getStrategyName() {
        return "MockAllocator";
    }
    
    @Override
    public long getTotalAllocated() {
        return totalAllocated.get();
    }
}