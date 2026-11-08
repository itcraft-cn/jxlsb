package cn.itcraft.jxlsb.spi.impl;

import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.impl.MemorySegmentAllocator;
import cn.itcraft.jxlsb.spi.AllocatorProvider;

/**
 * Java 23+内存分配器提供者
 * 
 * <p>使用MemorySegment + Foreign Memory API实现堆外内存管理。
 * 
 * <p>优先级20，高于ByteBufferAllocatorProvider的优先级10，
 * 在Java 23+环境中自动选择此实现。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class MemorySegmentAllocatorProvider implements AllocatorProvider {
    
    private static final String NAME = "MemorySegment-ForeignAPI-Provider";
    private static final int PRIORITY = 20;
    
    @Override
    public OffHeapAllocator createAllocator() {
        return new MemorySegmentAllocator();
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
}