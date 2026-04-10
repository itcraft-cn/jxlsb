package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.*;
import cn.itcraft.jxlsb.container.SheetInfo;
import cn.itcraft.jxlsb.format.*;
import cn.itcraft.jxlsb.data.CellType;
import java.nio.file.Path;
import java.io.*;
import java.util.*;

/**
 * XLSB文件流式读取器
 * 
 * <p>提供两种API风格：
 * <ul>
 *   <li>forEachSheet：遍历所有Sheet</li>
 *   <li>forEachRow：遍历指定Sheet的所有行</li>
 * </ul>
 * 
 * <p>使用Builder模式构造：
 * <pre>
 * XlsbReader reader = XlsbReader.builder()
 *     .path(Paths.get("input.xlsb"))
 *     .build();
 * </pre>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbReader implements AutoCloseable {
    
    private final XlsbContainerReader containerReader;
    private final SharedStringsTable sst;
    
    private XlsbReader(Path path) throws IOException {
        this.containerReader = new XlsbContainerReader(path);
        this.sst = loadSharedStringsTable();
    }
    
    private SharedStringsTable loadSharedStringsTable() throws IOException {
        SharedStringsTable table = new SharedStringsTable();
        
        if (containerReader.hasSharedStrings()) {
            try (InputStream stream = containerReader.getSharedStringsStream()) {
                table.load(stream);
            }
        }
        
        return table;
    }
    
    public List<SheetInfo> getSheetInfos() throws IOException {
        return containerReader.getSheetInfos();
    }
    
    public void forEachSheet(SheetConsumer consumer) throws Exception {
        List<SheetInfo> sheets = getSheetInfos();
        
        for (int i = 0; i < sheets.size(); i++) {
            SheetInfo info = sheets.get(i);
            
            try (SheetReader reader = new SheetReader(containerReader.getSheetStream(i), sst)) {
                consumer.accept(info, reader);
            }
        }
    }
    
    public void forEachRow(int sheetIndex, RowConsumer consumer) throws IOException {
        try (SheetReader reader = getSheetReader(sheetIndex)) {
            reader.readRows(new RowHandler() {
                private int currentRow = -1;
                
                @Override
                public void onRowStart(int rowIndex, int columnCount) {
                    currentRow = rowIndex;
                    consumer.onRowStart(rowIndex);
                }
                
                @Override
                public void onCellNumber(int row, int col, double value) {
                    consumer.onCell(row, col, CellData.number(value));
                }
                
                @Override
                public void onCellText(int row, int col, String value) {
                    consumer.onCell(row, col, CellData.text(value));
                }
                
                @Override
                public void onCellBoolean(int row, int col, boolean value) {
                    consumer.onCell(row, col, CellData.bool(value));
                }
                
                @Override
                public void onCellBlank(int row, int col) {
                    consumer.onCell(row, col, CellData.blank());
                }
                
                @Override
                public void onCellDate(int row, int col, double excelDate) {
                    consumer.onCell(row, col, CellData.date((long)excelDate));
                }
                
                @Override
                public void onRowEnd(int rowIndex) {
                    consumer.onRowEnd(rowIndex);
                }
            });
        }
    }
    
    private SheetReader getSheetReader(int sheetIndex) throws IOException {
        return new SheetReader(containerReader.getSheetStream(sheetIndex), sst);
    }
    
    @Override
    public void close() throws IOException {
        containerReader.close();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private Path path;
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public XlsbReader build() throws IOException {
            Objects.requireNonNull(path, "Path must not be null");
            return new XlsbReader(path);
        }
    }
}