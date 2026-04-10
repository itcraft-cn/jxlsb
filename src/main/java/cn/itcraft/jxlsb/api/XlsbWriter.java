package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.CellType;
import cn.itcraft.jxlsb.io.OffHeapOutputStream;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.format.XlsbFormatWriter;
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
    private final RecordWriter recordWriter;
    private final XlsbFormatWriter formatWriter;
    private int sheetCount = 0;
    
    private XlsbWriter(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.outputStream = new OffHeapOutputStream(builder.path);
        this.recordWriter = new RecordWriter(16 * 1024 * 1024, outputStream);
        this.formatWriter = new XlsbFormatWriter(recordWriter);
    }
    
    public OffHeapSheet createSheet(String sheetName, int rowCount, int columnCount) {
        return new OffHeapSheet(sheetName, sheetCount++, rowCount, columnCount);
    }
    
    public void writeSheet(OffHeapSheet sheet) throws IOException {
        try {
            formatWriter.writeSheet(sheet);
        } catch (Exception e) {
            throw new IOException("Failed to write sheet", e);
        }
    }
    
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                           int rowCount, int columnCount) throws IOException {
        formatWriter.writeSheetStart(sheetCount++);
        
        for (int row = 0; row < rowCount; row++) {
            formatWriter.writeRowStart(row, columnCount);
            
            for (int col = 0; col < columnCount; col++) {
                CellData data = supplier.get(row, col);
                if (data != null && data.getType() != null) {
                    formatWriter.writeCellDirect(row, col, data.getType(), data.getValue());
                }
            }
            
            formatWriter.writeRowEnd();
        }
        
        formatWriter.writeSheetEnd();
    }
    
    int getSheetCount() {
        return sheetCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public void close() throws IOException {
        try {
            formatWriter.close();
        } finally {
            try {
                recordWriter.close();
            } finally {
                outputStream.close();
            }
        }
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