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
 * <p>提供三种API风格：
 * <ul>
 *   <li>forEachSheet：遍历所有Sheet</li>
 *   <li>forEachRow：流式处理，回调方式</li>
 *   <li>readRows：分页读取，批量返回List/Array</li>
 * </ul>
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
    
    /**
     * 分页读取行数据（List形式）
     * 
     * <p>适合场景：批量处理、分页加载
     * 
     * @param sheetIndex Sheet索引（从0开始）
     * @param startRow 起始行号（从0开始）
     * @param batchSize 批量大小
     * @return 行数据列表，每行为CellData数组。如果无数据返回空列表。
     */
    public List<CellData[]> readRows(int sheetIndex, int startRow, int batchSize) throws IOException {
        final int fStartRow = startRow;
        final int fEndRow = startRow + batchSize - 1;
        List<CellData[]> result = new ArrayList<>(batchSize);
        
        try (SheetReader reader = getSheetReader(sheetIndex)) {
            reader.readRows(new RowHandler() {
                private CellData[] currentRowData;
                
                @Override
                public void onRowStart(int rowIndex, int colCount) {
                    if (rowIndex >= fStartRow && rowIndex <= fEndRow) {
                        currentRowData = new CellData[colCount];
                    } else if (rowIndex > fEndRow) {
                        throw new SheetReader.BatchCompleteException();
                    }
                }
                
                @Override
                public void onCellNumber(int row, int col, double value) {
                    if (currentRowData != null && col < currentRowData.length) {
                        currentRowData[col] = CellData.number(value);
                    }
                }
                
                @Override
                public void onCellText(int row, int col, String value) {
                    if (currentRowData != null && col < currentRowData.length) {
                        currentRowData[col] = CellData.text(value);
                    }
                }
                
                @Override
                public void onCellBoolean(int row, int col, boolean value) {
                    if (currentRowData != null && col < currentRowData.length) {
                        currentRowData[col] = CellData.bool(value);
                    }
                }
                
                @Override
                public void onCellBlank(int row, int col) {
                    if (currentRowData != null && col < currentRowData.length) {
                        currentRowData[col] = CellData.blank();
                    }
                }
                
                @Override
                public void onCellDate(int row, int col, double excelDate) {
                    if (currentRowData != null && col < currentRowData.length) {
                        currentRowData[col] = CellData.date((long)excelDate);
                    }
                }
                
                @Override
                public void onRowEnd(int rowIndex) {
                    if (currentRowData != null) {
                        result.add(currentRowData);
                        currentRowData = null;
                    }
                    
                    if (rowIndex >= fEndRow) {
                        throw new SheetReader.BatchCompleteException();
                    }
                }
            });
        } catch (SheetReader.BatchCompleteException e) {
            // 正常结束，读取到目标行数
        }
        
        return result;
    }
    
    /**
     * 分页读取行数据（数组形式）
     * 
     * @param sheetIndex Sheet索引
     * @param startRow 起始行号
     * @param batchSize 批量大小
     * @return 行数据数组
     */
    public CellData[][] readRowsAsArray(int sheetIndex, int startRow, int batchSize) throws IOException {
        List<CellData[]> list = readRows(sheetIndex, startRow, batchSize);
        return list.toArray(new CellData[0][]);
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