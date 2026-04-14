package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.XlsbContainer;
import cn.itcraft.jxlsb.container.XlsbContainerReader;
import cn.itcraft.jxlsb.container.ContentTypes;
import cn.itcraft.jxlsb.container.RelsGenerator;
import cn.itcraft.jxlsb.container.SheetInfo;
import cn.itcraft.jxlsb.container.XmlGenerator;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.format.WorkbookWriter;
import cn.itcraft.jxlsb.format.SheetWriter;
import cn.itcraft.jxlsb.format.StylesWriter;
import cn.itcraft.jxlsb.format.SheetReader;
import cn.itcraft.jxlsb.format.SheetParser;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Objects;

public final class XlsbWriter implements AutoCloseable {
    
    private final XlsbContainer container;
    private final SharedStringsTable sharedStrings;
    private final WorkbookWriter workbookWriter;
    private final StylesWriter stylesWriter;
    private final SheetWriter sheetWriter;
    private int sheetCount = 0;
    
    private String currentSheetName;
    private int currentColumnCount;
    private int currentRowCount;
    
    private final boolean isTemplateMode;
    private XlsbContainerReader templateReader;
    private int templateSheetCount;
    private int currentFillSheetIndex;
    private int fillStartRow;
    private int fillStartCol;
    private int fillRowCount;
    
    private List<SheetParser.CellInfo> streamingTemplateCells;
    private int streamingTemplateMaxRow;
    private int streamingTemplateMaxCol;
    private List<SheetParser.MergeCell> streamingMergeCells;
    private List<List<Object>> streamingAccumulatedData;
    
    private XlsbWriter(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Output path must not be null");
        this.container = XlsbContainer.create(builder.path);
        
        if (builder.template != null) {
            this.isTemplateMode = true;
            this.templateReader = new XlsbContainerReader(builder.template);
            this.sharedStrings = loadTemplateSST();
            this.stylesWriter = new StylesWriter();
            this.workbookWriter = new WorkbookWriter();
            this.sheetWriter = new SheetWriter(sharedStrings, stylesWriter);
            this.templateSheetCount = templateReader.getSheetInfos().size();
            copyTemplateStructure();
        } else {
            this.isTemplateMode = false;
            this.templateReader = null;
            this.sharedStrings = new SharedStringsTable();
            this.stylesWriter = new StylesWriter();
            this.workbookWriter = new WorkbookWriter();
            this.sheetWriter = new SheetWriter(sharedStrings, stylesWriter);
            this.templateSheetCount = 0;
        }
        
        this.currentFillSheetIndex = -1;
    }
    
    private SharedStringsTable loadTemplateSST() throws IOException {
        SharedStringsTable table = new SharedStringsTable();
        if (templateReader.hasSharedStrings()) {
            try (InputStream stream = templateReader.getSharedStringsStream()) {
                table.load(stream);
            }
        }
        return table;
    }
    
    private void copyTemplateStructure() throws IOException {
        Set<String> entries = templateReader.getAllEntryNames();
        
        for (String name : entries) {
            if (name.startsWith("xl/worksheets/") || 
                name.equals("xl/sharedStrings.bin")) {
                continue;
            }
            
            byte[] data = templateReader.getEntryBytes(name);
            if (data != null) {
                container.addEntry(name, data);
            }
        }
        
        List<SheetInfo> sheets = templateReader.getSheetInfos();
        for (SheetInfo info : sheets) {
            workbookWriter.addSheet(info.getName());
        }
        
        sheetCount = templateSheetCount;
    }
    
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                           int rowCount, int columnCount) throws IOException {
        if (isTemplateMode) {
            throw new IllegalStateException("Template mode: use fillBatch() instead of writeBatch()");
        }
        
        workbookWriter.addSheet(sheetName);
        byte[] sheetData = sheetWriter.writeSheet(supplier, rowCount, columnCount);
        container.addEntry("xl/worksheets/sheet" + (sheetCount + 1) + ".bin", sheetData);
        sheetCount++;
    }
    
    public void startSheet(String sheetName, int columnCount) throws IOException {
        if (isTemplateMode) {
            throw new IllegalStateException("Template mode: use startFill() instead of startSheet()");
        }
        
        if (currentSheetName != null) {
            throw new IllegalStateException("Previous sheet not ended, call endSheet() first");
        }
        this.currentSheetName = sheetName;
        this.currentColumnCount = columnCount;
        this.currentRowCount = 0;
        sheetWriter.startStreaming(columnCount);
    }
    
    public <T> void writeRows(List<T> dataList, int startRow, RowDataSupplier<T> supplier) 
            throws IOException {
        if (isTemplateMode) {
            throw new IllegalStateException("Template mode: use fillRows() instead of writeRows()");
        }
        
        if (currentSheetName == null) {
            throw new IllegalStateException("Sheet not started, call startSheet() first");
        }
        
        int batchSize = dataList.size();
        CellDataSupplier cellSupplier = (row, col) -> {
            int index = row - startRow;
            if (index >= 0 && index < dataList.size()) {
                return supplier.get(dataList.get(index), col);
            }
            return CellData.blank();
        };
        
        sheetWriter.appendRows(cellSupplier, startRow, batchSize, currentColumnCount);
        currentRowCount = Math.max(currentRowCount, startRow + batchSize);
    }
    
    public <T> void writeRows(T[] dataArray, int startRow, RowDataSupplier<T> supplier) 
            throws IOException {
        if (isTemplateMode) {
            throw new IllegalStateException("Template mode: use fillRows() instead of writeRows()");
        }
        
        if (currentSheetName == null) {
            throw new IllegalStateException("Sheet not started, call startSheet() first");
        }
        
        int batchSize = dataArray.length;
        CellDataSupplier cellSupplier = (row, col) -> {
            int index = row - startRow;
            if (index >= 0 && index < dataArray.length) {
                return supplier.get(dataArray[index], col);
            }
            return CellData.blank();
        };
        
        sheetWriter.appendRows(cellSupplier, startRow, batchSize, currentColumnCount);
        currentRowCount = Math.max(currentRowCount, startRow + batchSize);
    }
    
    public void endSheet() throws IOException {
        if (isTemplateMode) {
            throw new IllegalStateException("Template mode: use endFill() instead of endSheet()");
        }
        
        if (currentSheetName == null) {
            throw new IllegalStateException("Sheet not started");
        }
        
        workbookWriter.addSheet(currentSheetName);
        byte[] sheetData = sheetWriter.finalizeStreaming(currentRowCount, currentColumnCount);
        container.addEntry("xl/worksheets/sheet" + (sheetCount + 1) + ".bin", sheetData);
        sheetCount++;
        
        currentSheetName = null;
        currentColumnCount = 0;
        currentRowCount = 0;
    }
    
    public void fillBatch(int sheetIndex, List<?> dataList, int startRow, int startCol) throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode: use writeBatch() instead");
        }
        
        Objects.requireNonNull(dataList, "dataList must not be null");
        if (sheetIndex < 0 || sheetIndex >= templateSheetCount) {
            throw new IllegalArgumentException("Invalid sheet index: " + sheetIndex);
        }
        
        SheetParser parser = new SheetParser(templateReader.getSheetStream(sheetIndex), sharedStrings);
        try {
            List<cn.itcraft.jxlsb.format.SheetParser.CellInfo> templateCells = parser.parse();
            
            int templateMaxRow = parser.getMaxRow();
            int templateMaxCol = parser.getMaxCol();
            List<cn.itcraft.jxlsb.format.SheetParser.MergeCell> mergeCells = parser.getMergeCells();
            
            int columnCount = Math.max(templateMaxCol + 1, 4);
            
            CellDataSupplier supplier = (row, col) -> {
                int index = row - startRow;
                if (index >= 0 && index < dataList.size()) {
                    Object rowData = dataList.get(index);
                    if (rowData instanceof List) {
                        List<?> rowList = (List<?>) rowData;
                        int colIndex = col - startCol;
                        if (colIndex >= 0 && colIndex < rowList.size()) {
                            return toCellData(rowList.get(colIndex), col);
                        }
                    } else {
                        return toCellData(rowData, col);
                    }
                }
                return CellData.blank();
            };
            
            byte[] sheetData = sheetWriter.writeSheetWithTemplate(
                supplier, dataList.size(), columnCount, startRow, startCol, 
                templateCells, templateMaxRow, templateMaxCol, mergeCells);
            
            container.addEntry("xl/worksheets/sheet" + (sheetIndex + 1) + ".bin", sheetData);
        } finally {
            parser.close();
        }
    }
    
    public void fillBatch(List<?> dataList, int startRow, int startCol) throws IOException {
        fillBatch(0, dataList, startRow, startCol);
    }
    
    public void fillAtMarker(int sheetIndex, String marker, List<?> dataList) throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        
        Objects.requireNonNull(marker, "marker must not be null");
        Objects.requireNonNull(dataList, "dataList must not be null");
        
        MarkerPosition pos = findMarker(sheetIndex, marker);
        if (pos == null) {
            throw new IllegalArgumentException("Marker not found: " + marker);
        }
        
        fillBatch(sheetIndex, dataList, pos.row, pos.col);
    }
    
    public void fillAtMarker(String marker, List<?> dataList) throws IOException {
        fillAtMarker(0, marker, dataList);
    }
    
    public void startFill(int sheetIndex, int startRow, int startCol) throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        
        if (currentFillSheetIndex >= 0) {
            throw new IllegalStateException("Previous fill not ended, call endFill() first");
        }
        
        if (sheetIndex < 0 || sheetIndex >= templateSheetCount) {
            throw new IllegalArgumentException("Invalid sheet index: " + sheetIndex);
        }
        
        this.currentFillSheetIndex = sheetIndex;
        this.fillStartRow = startRow;
        this.fillStartCol = startCol;
        this.fillRowCount = 0;
        
        SheetParser parser = new SheetParser(
            this.templateReader.getSheetStream(sheetIndex), sharedStrings);
        try {
            this.streamingTemplateCells = parser.parse();
            this.streamingTemplateMaxRow = parser.getMaxRow();
            this.streamingTemplateMaxCol = parser.getMaxCol();
            this.streamingMergeCells = parser.getMergeCells();
        } finally {
            parser.close();
        }
        this.streamingAccumulatedData = new ArrayList<>();
    }
    
    public void fillRows(List<?> dataList) throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        
        if (currentFillSheetIndex < 0) {
            throw new IllegalStateException("Fill not started, call startFill() first");
        }
        
        for (Object item : dataList) {
            if (item instanceof List) {
                streamingAccumulatedData.add((List<Object>) item);
            } else {
                List<Object> row = new ArrayList<>();
                row.add(item);
                streamingAccumulatedData.add(row);
            }
        }
        fillRowCount = streamingAccumulatedData.size();
    }
    
    public void endFill() throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        
        if (currentFillSheetIndex < 0) {
            throw new IllegalStateException("Fill not started");
        }
        
        int columnCount = Math.max(streamingTemplateMaxCol + 1 - fillStartCol, 4);
        
        CellDataSupplier supplier = (row, col) -> {
            int index = row - fillStartRow;
            if (index >= 0 && index < streamingAccumulatedData.size()) {
                List<Object> rowList = streamingAccumulatedData.get(index);
                int colIndex = col - fillStartCol;
                if (colIndex >= 0 && colIndex < rowList.size()) {
                    return toCellData(rowList.get(colIndex), col);
                }
            }
            return CellData.blank();
        };
        
        byte[] sheetData = sheetWriter.writeSheetWithTemplate(
            supplier, fillRowCount, columnCount, fillStartRow, fillStartCol,
            streamingTemplateCells, streamingTemplateMaxRow, streamingTemplateMaxCol,
            streamingMergeCells);
        
        container.addEntry("xl/worksheets/sheet" + (currentFillSheetIndex + 1) + ".bin", sheetData);
        
        currentFillSheetIndex = -1;
        fillStartRow = 0;
        fillStartCol = 0;
        fillRowCount = 0;
        streamingTemplateCells = null;
        streamingTemplateMaxRow = 0;
        streamingTemplateMaxCol = 0;
        streamingMergeCells = null;
        streamingAccumulatedData = null;
    }
    
    private MarkerPosition findMarker(int sheetIndex, String marker) throws IOException {
        try (SheetReader reader = new SheetReader(
                templateReader.getSheetStream(sheetIndex), sharedStrings)) {
            
            final MarkerPosition[] result = new MarkerPosition[1];
            
            reader.readRows(new cn.itcraft.jxlsb.api.RowHandler() {
                
                @Override
                public void onRowStart(int rowIndex, int columnCount) {
                }
                
                @Override
                public void onCellNumber(int row, int col, double value) {
                }
                
                @Override
                public void onCellText(int row, int col, String value) {
                    if (value != null && value.equals(marker)) {
                        result[0] = new MarkerPosition(row, col);
                    }
                }
                
                @Override
                public void onCellBoolean(int row, int col, boolean value) {
                }
                
                @Override
                public void onCellBlank(int row, int col) {
                }
                
                @Override
                public void onCellDate(int row, int col, double excelDate) {
                }
                
                @Override
                public void onRowEnd(int rowIndex) {
                }
            });
            
            return result[0];
        }
    }
    
    private CellData toCellData(Object obj, int col) {
        if (obj == null) {
            return CellData.blank();
        }
        
        if (obj instanceof CellData) {
            return (CellData) obj;
        }
        
        if (obj instanceof Number) {
            return CellData.number(((Number) obj).doubleValue());
        }
        
        if (obj instanceof String) {
            return CellData.text((String) obj);
        }
        
        if (obj instanceof Boolean) {
            return CellData.bool((Boolean) obj);
        }
        
        return CellData.text(obj.toString());
    }
    
    public int getTemplateSheetCount() {
        return templateSheetCount;
    }
    
    public List<SheetInfo> getTemplateSheetInfos() throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        return templateReader.getSheetInfos();
    }
    
    int getSheetCount() {
        return sheetCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public void close() throws IOException {
        if (isTemplateMode) {
            updateSharedStrings();
        } else if (sheetCount > 0) {
            writeContainerStructure();
        }
        
        if (isTemplateMode && templateReader != null) {
            templateReader.close();
        }
        
        container.close();
    }
    
    private void updateSharedStrings() throws IOException {
        if (sharedStrings.getCount() > 0) {
            byte[] sstData = sharedStrings.toBiff12Bytes();
            container.addEntry("xl/sharedStrings.bin", sstData);
        }
    }
    
    private void writeContainerStructure() throws IOException {
        ContentTypes ct = new ContentTypes();
        ct.addOverride("/docProps/app.xml", "application/vnd.openxmlformats-officedocument.extended-properties+xml");
        ct.addOverride("/docProps/core.xml", "application/vnd.openxmlformats-package.core-properties+xml");
        ct.addOverride("/xl/sharedStrings.bin", "application/vnd.ms-excel.sharedStrings");
        ct.addOverride("/xl/styles.bin", "application/vnd.ms-excel.styles");
        ct.addOverride("/xl/theme/theme1.xml", "application/vnd.openxmlformats-officedocument.theme+xml");
        for (int i = 1; i <= sheetCount; i++) {
            ct.addOverride("/xl/worksheets/sheet" + i + ".bin", 
                          "application/vnd.ms-excel.worksheet");
        }
        container.addEntry("[Content_Types].xml", ct.toXml());
        
        container.addEntry("_rels/.rels", RelsGenerator.generateRootRels());
        container.addEntry("docProps/app.xml", XmlGenerator.generateAppXml(sheetCount));
        container.addEntry("docProps/core.xml", XmlGenerator.generateCoreXml());
        container.addEntry("xl/workbook.bin", workbookWriter.toBiff12Bytes());
        container.addEntry("xl/styles.bin", stylesWriter.toBiff12Bytes());
        container.addEntry("xl/theme/theme1.xml", XmlGenerator.generateThemeXml());
        container.addEntry("xl/_rels/workbook.bin.rels", 
            RelsGenerator.generateWorkbookRels(sheetCount, sharedStrings.getCount() > 0));
        
        if (sharedStrings.getCount() > 0) {
            container.addEntry("xl/sharedStrings.bin", sharedStrings.toBiff12Bytes());
        }
    }
    
    
    
    private static final class MarkerPosition {
        final int row;
        final int col;
        
        MarkerPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
    
    public static final class Builder {
        private Path template;
        private Path path;
        
        public Builder template(Path template) {
            this.template = template;
            return this;
        }
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public XlsbWriter build() throws IOException {
            return new XlsbWriter(this);
        }
    }
}