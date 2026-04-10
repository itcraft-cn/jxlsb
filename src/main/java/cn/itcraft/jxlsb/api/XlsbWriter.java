package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.io.OffHeapOutputStream;
import cn.itcraft.jxlsb.format.RecordWriter;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Objects;

/**
 * XLSB文件写入器
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbWriter implements AutoCloseable {
    
    private final OffHeapOutputStream outputStream;
    private int sheetCount = 0;
    
    private XlsbWriter(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.outputStream = new OffHeapOutputStream(builder.path);
    }
    
    public OffHeapSheet createSheet(String sheetName, int rowCount, int columnCount) {
        return new OffHeapSheet(sheetName, sheetCount++, rowCount, columnCount);
    }
    
    public void writeSheet(OffHeapSheet sheet) throws IOException {
    }
    
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                          int rowCount, int columnCount) throws IOException {
        OffHeapSheet sheet = createSheet(sheetName, rowCount, columnCount);
        
        for (int row = 0; row < rowCount; row++) {
            OffHeapRow currentRow = sheet.createRow(row);
            for (int col = 0; col < columnCount; col++) {
                CellData data = supplier.get(row, col);
                OffHeapCell cell = currentRow.getCell(col);
                setCellData(cell, data);
            }
        }
        
        sheet.close();
    }
    
    private void setCellData(OffHeapCell cell, CellData data) {
        if (data == null || data.getType() == null) {
            return;
        }
        
        switch (data.getType()) {
            case TEXT:
                cell.setText((String) data.getValue());
                break;
            case NUMBER:
                cell.setNumber((Double) data.getValue());
                break;
            case DATE:
                cell.setDate((Long) data.getValue());
                break;
            case BOOLEAN:
                cell.setBoolean((Boolean) data.getValue());
                break;
            default:
                break;
        }
    }
    
    int getSheetCount() {
        return sheetCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public void close() throws IOException {
        outputStream.close();
    }
    
    public static final class Builder {
        private Path path;
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public XlsbWriter build() throws IOException {
            return new XlsbWriter(this);
        }
    }
}