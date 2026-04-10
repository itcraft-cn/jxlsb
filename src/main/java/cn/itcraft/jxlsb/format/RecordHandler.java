package cn.itcraft.jxlsb.format;

/**
 * Record处理器接口
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface RecordHandler {
    
    void handle(BiffRecord record) throws Exception;
}