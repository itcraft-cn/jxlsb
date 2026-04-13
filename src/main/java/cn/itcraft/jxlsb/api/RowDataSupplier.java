package cn.itcraft.jxlsb.api;

/**
 * 行数据供应接口（用于流式追加）
 * 
 * <p>接收数据对象和列索引，返回CellData。
 * 
 * @param <T> 数据类型
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface RowDataSupplier<T> {
    
    CellData get(T data, int col);
}