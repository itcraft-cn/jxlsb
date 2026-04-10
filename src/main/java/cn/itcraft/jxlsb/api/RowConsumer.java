package cn.itcraft.jxlsb.api;

/**
 * Row处理回调接口（简化版）
 * 
 * <p>用于forEachRow方法，统一处理单元格数据。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public interface RowConsumer {
    
    void onRowStart(int rowIndex);
    
    void onCell(int row, int col, CellData data);
    
    void onRowEnd(int rowIndex);
}