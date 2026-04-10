package cn.itcraft.jxlsb.memory;

/**
 * 堆外内存分配器接口
 * 
 * <p>定义堆外内存分配、释放、访问的抽象操作。
 * Java 8和Java 17有不同实现策略：
 * - Java 8: ByteBuffer.allocateDirect() + sun.misc.Unsafe
 * - Java 17: MemorySegment.allocateNative() + Foreign Memory API
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public interface OffHeapAllocator {
    
    /**
     * 分配指定大小的堆外内存块
     */
    MemoryBlock allocate(long size);
    
    /**
     * 从内存池获取可复用内存块
     */
    MemoryBlock allocateFromPool(long size);
    
    /**
     * 获取当前内存分配器策略名称
     */
    String getStrategyName();
    
    /**
     * 获取当前已分配的总堆外内存大小
     */
    long getTotalAllocated();
    
    /**
     * 释放指定内存块
     */
    void deallocate(MemoryBlock block);
}