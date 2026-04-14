package cn.itcraft.jxlsb.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Queue;

/**
 * 堆外内存池
 * 
 * <p>管理可复用的堆外内存块，避免频繁分配释放。
 * 使用分段池策略，不同大小的内存块分类管理。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class MemoryPool implements AutoCloseable {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryPool.class);
    
    private static final long[] SIZE_CLASSES = {
        64L,
        4 * 1024L,
        64 * 1024L,
        1024 * 1024L,
        16 * 1024 * 1024L
    };
    
    private final OffHeapAllocator allocator;
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<MemoryBlock>> pool;
    private final AtomicLong totalPooledSize = new AtomicLong(0);
    
    public MemoryPool(OffHeapAllocator allocator) {
        this.allocator = allocator;
        this.pool = new ConcurrentHashMap<>();
    }
    
    public MemoryBlock acquire(long size) {
        long classSize = findSizeClass(size);
        Queue<MemoryBlock> queue = pool.get(classSize);
        
        if (queue != null) {
            MemoryBlock block = queue.poll();
            if (block != null) {
                totalPooledSize.addAndGet(-classSize);
                return new PooledMemoryBlock(block, classSize, this);
            }
        }
        
        MemoryBlock newBlock = allocator.allocate(classSize);
        return new PooledMemoryBlock(newBlock, classSize, this);
    }
    
    void release(MemoryBlock block, long classSize) {
        if (block != null) {
            Queue<MemoryBlock> queue = pool.computeIfAbsent(
                classSize, k -> new ConcurrentLinkedQueue<>());
            queue.offer(block);
            totalPooledSize.addAndGet(classSize);
        }
    }
    
    @Override
    public void close() {
        pool.forEach((size, queue) -> {
            MemoryBlock block;
            while ((block = queue.poll()) != null) {
                try {
                    allocator.deallocate(block);
                } catch (Exception e) {
                    log.warn("Failed to deallocate memory block of size {}", size, e);
                }
            }
        });
        pool.clear();
        totalPooledSize.set(0);
    }
    
    public long getTotalPooledSize() {
        return totalPooledSize.get();
    }
    
    private long findSizeClass(long size) {
        for (long classSize : SIZE_CLASSES) {
            if (size <= classSize) {
                return classSize;
            }
        }
        return size;
    }
}