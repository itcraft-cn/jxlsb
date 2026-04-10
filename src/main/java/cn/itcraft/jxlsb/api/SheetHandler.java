package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.OffHeapSheet;

/**
 * Sheet处理器接口
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface SheetHandler {
    
    void handle(OffHeapSheet sheet) throws Exception;
}