package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.OffHeapCell;

/**
 * Cell处理器接口
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface CellHandler {
    
    void handle(OffHeapCell cell) throws Exception;
}