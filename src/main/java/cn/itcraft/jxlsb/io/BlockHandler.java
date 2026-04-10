package cn.itcraft.jxlsb.io;

import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * 内存块处理器接口
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface BlockHandler {
    
    void handle(MemoryBlock block) throws Exception;
}