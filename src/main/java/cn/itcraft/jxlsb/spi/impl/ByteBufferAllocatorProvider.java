package cn.itcraft.jxlsb.spi.impl;

import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.impl.ByteBufferAllocator;
import cn.itcraft.jxlsb.spi.AllocatorProvider;

/**
 * Java 8+内存分配器提供者
 * 
 * <p>使用ByteBuffer实现堆外内存管理。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class ByteBufferAllocatorProvider implements AllocatorProvider {
    
    private static final String NAME = "ByteBuffer-Direct-Provider";
    private static final int PRIORITY = 10;
    
    @Override
    public OffHeapAllocator createAllocator() {
        return new ByteBufferAllocator();
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