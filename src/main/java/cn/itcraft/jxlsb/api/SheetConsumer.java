package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.SheetInfo;
import cn.itcraft.jxlsb.format.SheetReader;

/**
 * Sheet处理回调接口
 * 
 * <p>用于forEachSheet方法，处理每个Sheet。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface SheetConsumer {
    
    void accept(SheetInfo info, SheetReader reader) throws Exception;
}