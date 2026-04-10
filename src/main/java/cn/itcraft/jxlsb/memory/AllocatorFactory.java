package cn.itcraft.jxlsb.memory;

import cn.itcraft.jxlsb.spi.AllocatorProvider;
import cn.itcraft.jxlsb.spi.impl.ByteBufferAllocatorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ServiceLoader;

/**
 * 内存分配器工厂
 * 
 * <p>根据JDK版本自动加载最优的内存分配器实现。
 * 使用ServiceLoader机制发现所有AllocatorProvider实现，
 * 选择优先级最高的作为默认分配器。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class AllocatorFactory {
    
    private static final Logger log = LoggerFactory.getLogger(AllocatorFactory.class);
    
    private static final AllocatorProvider DEFAULT_PROVIDER;
    
    static {
        DEFAULT_PROVIDER = loadBestProvider();
        log.info("Loaded allocator provider: {} with priority {}", 
                DEFAULT_PROVIDER.getName(), DEFAULT_PROVIDER.getPriority());
    }
    
    private static AllocatorProvider loadBestProvider() {
        ServiceLoader<AllocatorProvider> loader = 
            ServiceLoader.load(AllocatorProvider.class);
        
        AllocatorProvider best = null;
        int maxPriority = -1;
        
        for (AllocatorProvider provider : loader) {
            if (provider.getPriority() > maxPriority) {
                maxPriority = provider.getPriority();
                best = provider;
            }
        }
        
        if (best == null) {
            log.warn("No AllocatorProvider found via ServiceLoader, using ByteBufferAllocatorProvider as fallback");
            best = new ByteBufferAllocatorProvider();
        }
        
        return best;
    }
    
    /**
     * 创建默认内存分配器
     */
    public static OffHeapAllocator createDefaultAllocator() {
        return DEFAULT_PROVIDER.createAllocator();
    }
    
    /**
     * 获取默认提供者
     */
    public static AllocatorProvider getDefaultProvider() {
        return DEFAULT_PROVIDER;
    }
    
    private AllocatorFactory() {
    }
}