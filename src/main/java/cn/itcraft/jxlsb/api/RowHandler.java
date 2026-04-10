package cn.itcraft.jxlsb.api;

/**
 * Row处理回调接口
 * 
 * <p>流式处理Sheet行数据的函数式接口。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public interface RowHandler {
    
    void onRowStart(int rowIndex, int columnCount);
    
    void onRowEnd(int rowIndex);
    
    void onCellNumber(int row, int col, double value);
    
    void onCellText(int row, int col, String value);
    
    void onCellBoolean(int row, int col, boolean value);
    
    void onCellBlank(int row, int col);
    
    void onCellDate(int row, int col, double excelDate);
}