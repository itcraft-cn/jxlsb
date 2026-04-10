package cn.itcraft.jxlsb.spi;

import cn.itcraft.jxlsb.memory.OffHeapAllocator;

/**
 * 内存分配器服务提供者接口
 * 
 * <p>通过ServiceLoader机制自动加载最优实现。
 * 不同JDK版本提供不同的AllocatorProvider实现。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public interface AllocatorProvider {
    
    /**
     * 创建内存分配器实例
     */
    OffHeapAllocator createAllocator();
    
    /**
     * 获取提供者名称
     */
    String getName();
    
    /**
     * 获取优先级
     * 
     * <p>数值越大优先级越高
     */
    int getPriority();
}