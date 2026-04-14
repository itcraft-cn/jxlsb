package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.XlsbContainer;
import cn.itcraft.jxlsb.container.XlsbContainerReader;
import cn.itcraft.jxlsb.container.ContentTypes;
import cn.itcraft.jxlsb.container.RelsGenerator;
import cn.itcraft.jxlsb.container.SheetInfo;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.format.WorkbookWriter;
import cn.itcraft.jxlsb.format.SheetWriter;
import cn.itcraft.jxlsb.format.StylesWriter;
import cn.itcraft.jxlsb.format.SheetReader;
import cn.itcraft.jxlsb.format.TemplateSheetReader;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
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
            if (name.startsWith("xl/worksheets/")) {
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
        
        TemplateSheetReader templateReader = new TemplateSheetReader(
            this.templateReader.getSheetStream(sheetIndex), sharedStrings);
        int columnCount = templateReader.getColumnCount();
        
        CellDataSupplier supplier = (row, col) -> {
            int index = row - startRow;
            if (index >= 0 && index < dataList.size()) {
                Object obj = dataList.get(index);
                return toCellData(obj, col);
            }
            return CellData.blank();
        };
        
        byte[] sheetData = sheetWriter.writeSheetWithTemplate(
            supplier, dataList.size(), columnCount, startRow, startCol, templateReader);
        
        container.addEntry("xl/worksheets/sheet" + (sheetIndex + 1) + ".bin", sheetData);
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
        
        TemplateSheetReader templateReader = new TemplateSheetReader(
            this.templateReader.getSheetStream(sheetIndex), sharedStrings);
        int columnCount = templateReader.getColumnCount();
        sheetWriter.startStreaming(columnCount);
    }
    
    public void fillRows(List<?> dataList) throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        
        if (currentFillSheetIndex < 0) {
            throw new IllegalStateException("Fill not started, call startFill() first");
        }
        
        int batchSize = dataList.size();
        int actualStartRow = fillStartRow + fillRowCount;
        
        CellDataSupplier supplier = (row, col) -> {
            int index = row - actualStartRow;
            if (index >= 0 && index < dataList.size()) {
                Object obj = dataList.get(index);
                return toCellData(obj, col);
            }
            return CellData.blank();
        };
        
        sheetWriter.appendRows(supplier, actualStartRow, batchSize, 
            sheetWriter.getStreamingColumnCount());
        fillRowCount += batchSize;
    }
    
    public void endFill() throws IOException {
        if (!isTemplateMode) {
            throw new IllegalStateException("Not in template mode");
        }
        
        if (currentFillSheetIndex < 0) {
            throw new IllegalStateException("Fill not started");
        }
        
        byte[] sheetData = sheetWriter.finalizeStreaming(fillRowCount, 
            sheetWriter.getStreamingColumnCount());
        container.addEntry("xl/worksheets/sheet" + (currentFillSheetIndex + 1) + ".bin", sheetData);
        
        currentFillSheetIndex = -1;
        fillStartRow = 0;
        fillStartCol = 0;
        fillRowCount = 0;
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
        if (!isTemplateMode && sheetCount > 0) {
            writeContainerStructure();
        }
        
        if (isTemplateMode && templateReader != null) {
            templateReader.close();
        }
        
        container.close();
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
        container.addEntry("docProps/app.xml", generateAppXml());
        container.addEntry("docProps/core.xml", generateCoreXml());
        container.addEntry("xl/workbook.bin", workbookWriter.toBiff12Bytes());
        container.addEntry("xl/styles.bin", stylesWriter.toBiff12Bytes());
        container.addEntry("xl/theme/theme1.xml", generateThemeXml());
        container.addEntry("xl/_rels/workbook.bin.rels", 
            RelsGenerator.generateWorkbookRels(sheetCount, sharedStrings.getCount() > 0));
        
        if (sharedStrings.getCount() > 0) {
            container.addEntry("xl/sharedStrings.bin", sharedStrings.toBiff12Bytes());
        }
    }
    
    private byte[] generateAppXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\" ");
        sb.append("xmlns:vt=\"http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes\">");
        sb.append("<Application>jxlsb</Application>");
        sb.append("<HeadingPairs><vt:vector size=\"2\" baseType=\"variant\">");
        sb.append("<vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant>");
        sb.append("<vt:variant><vt:i4>").append(sheetCount).append("</vt:i4></vt:variant>");
        sb.append("</vt:vector></HeadingPairs>");
        sb.append("<TitlesOfParts><vt:vector size=\"").append(sheetCount).append("\" baseType=\"lpstr\">");
        for (int i = 0; i < sheetCount; i++) {
            sb.append("<vt:lpstr>Sheet").append(i + 1).append("</vt:lpstr>");
        }
        sb.append("</vt:vector></TitlesOfParts></Properties>");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateCoreXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" ");
        sb.append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ");
        sb.append("xmlns:dcterms=\"http://purl.org/dc/terms/\" ");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<dc:creator>jxlsb</dc:creator>");
        sb.append("<cp:lastModifiedBy>jxlsb</cp:lastModifiedBy>");
        sb.append("<dc:description>created by jxlsb</dc:description>");
        String now = java.time.Instant.now().toString();
        sb.append("<dcterms:created xsi:type=\"dcterms:W3CDTF\">").append(now).append("</dcterms:created>");
        sb.append("<dcterms:modified xsi:type=\"dcterms:W3CDTF\">").append(now).append("</dcterms:modified>");
        sb.append("</cp:coreProperties>");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private byte[] generateThemeXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Office Theme\">");
        sb.append("<a:themeElements>");
        sb.append("<a:clrScheme name=\"Office\">");
        sb.append("<a:dk1><a:sysClr val=\"windowText\" lastClr=\"000000\"/></a:dk1>");
        sb.append("<a:lt1><a:sysClr val=\"window\" lastClr=\"FFFFFF\"/></a:lt1>");
        sb.append("<a:dk2><a:srgbClr val=\"1F497D\"/></a:dk2>");
        sb.append("<a:lt2><a:srgbClr val=\"EEECE1\"/></a:lt2>");
        sb.append("<a:accent1><a:srgbClr val=\"4F81BD\"/></a:accent1>");
        sb.append("<a:accent2><a:srgbClr val=\"C0504D\"/></a:accent2>");
        sb.append("<a:accent3><a:srgbClr val=\"9BBB59\"/></a:accent3>");
        sb.append("<a:accent4><a:srgbClr val=\"8064A2\"/></a:accent4>");
        sb.append("<a:accent5><a:srgbClr val=\"4BACC6\"/></a:accent5>");
        sb.append("<a:accent6><a:srgbClr val=\"F79646\"/></a:accent6>");
        sb.append("<a:hlink><a:srgbClr val=\"0000FF\"/></a:hlink>");
        sb.append("<a:folHlink><a:srgbClr val=\"800080\"/></a:folHlink>");
        sb.append("</a:clrScheme>");
        sb.append("<a:fontScheme name=\"Office\">");
        sb.append("<a:majorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:majorFont>");
        sb.append("<a:minorFont><a:latin typeface=\"Calibri\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:minorFont>");
        sb.append("</a:fontScheme>");
        sb.append("<a:fmtScheme name=\"Office\">");
        sb.append("<a:fillStyleLst><a:noFill/><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill></a:fillStyleLst>");
        sb.append("<a:lnStyleLst><a:ln><a:noFill/></a:ln></a:lnStyleLst>");
        sb.append("<a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>");
        sb.append("<a:bgFillStyleLst><a:noFill/></a:bgFillStyleLst>");
        sb.append("</a:fmtScheme>");
        sb.append("</a:themeElements></a:theme>");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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