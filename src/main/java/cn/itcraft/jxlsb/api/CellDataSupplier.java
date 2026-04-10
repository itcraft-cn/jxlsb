package cn.itcraft.jxlsb.api;

/**
 * Cell数据供应接口
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@FunctionalInterface
public interface CellDataSupplier {
    
    CellData get(int row, int col);
}