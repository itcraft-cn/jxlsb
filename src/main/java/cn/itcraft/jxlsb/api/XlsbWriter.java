package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.XlsbContainer;
import cn.itcraft.jxlsb.container.ContentTypes;
import cn.itcraft.jxlsb.container.RelsGenerator;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.format.WorkbookWriter;
import cn.itcraft.jxlsb.format.SheetWriter;
import cn.itcraft.jxlsb.format.StylesWriter;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * XLSB文件写入器
 * 
 * <p>提供两种API风格：
 * <ul>
 *   <li>writeBatch：一次性写入，适合内存数据或实时计算</li>
 *   <li>startSheet + writeRows + endSheet：流式追加，适合数据库分页查询</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbWriter implements AutoCloseable {
    
    private final XlsbContainer container;
    private final SharedStringsTable sharedStrings;
    private final WorkbookWriter workbookWriter;
    private final SheetWriter sheetWriter;
    private int sheetCount = 0;
    
    private String currentSheetName;
    private int currentColumnCount;
    private int currentRowCount;
    private byte[] currentSheetData;
    
    private XlsbWriter(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.container = XlsbContainer.create(builder.path);
        this.sharedStrings = new SharedStringsTable();
        this.workbookWriter = new WorkbookWriter();
        this.sheetWriter = new SheetWriter(sharedStrings);
    }
    
    /**
     * 批量写入Sheet数据（一次性）
     * 
     * <p>适合场景：内存已有数据、或可实时计算生成数据
     * 
     * @param sheetName Sheet名称
     * @param supplier 数据供应函数，接收(row, col)返回CellData
     * @param rowCount 行数
     * @param columnCount 列数
     */
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                           int rowCount, int columnCount) throws IOException {
        workbookWriter.addSheet(sheetName);
        
        byte[] sheetData = sheetWriter.writeSheet(supplier, rowCount, columnCount);
        container.addEntry("xl/worksheets/sheet" + (sheetCount + 1) + ".bin", sheetData);
        sheetCount++;
    }
    
    /**
     * 开始Sheet流式写入
     * 
     * <p>适合场景：数据库分页查询、大文件流式处理
     * 
     * <p>使用示例：
     * <pre>
     * writer.startSheet("Orders", 5);
     * int offset = 0;
     * while (true) {
     *     List<Order> batch = db.query(offset, 1000);
     *     if (batch.isEmpty()) break;
     *     writer.writeRows(batch, offset, (order, col) -> ...);
     *     offset += batch.size();
     * }
     * writer.endSheet();
     * </pre>
     * 
     * @param sheetName Sheet名称
     * @param columnCount 列数（固定）
     */
    public void startSheet(String sheetName, int columnCount) throws IOException {
        if (currentSheetName != null) {
            throw new IllegalStateException("Previous sheet not ended, call endSheet() first");
        }
        this.currentSheetName = sheetName;
        this.currentColumnCount = columnCount;
        this.currentRowCount = 0;
        
        sheetWriter.startStreaming(columnCount);
    }
    
    /**
     * 流式追加行数据
     * 
     * @param dataList 数据列表
     * @param startRow 起始行号（从0开始）
     * @param supplier 数据供应函数，接收(data, col)返回CellData
     */
    public <T> void writeRows(List<T> dataList, int startRow, RowDataSupplier<T> supplier) 
            throws IOException {
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
    
    /**
     * 流式追加行数据（数组）
     * 
     * @param dataArray 数据数组
     * @param startRow 起始行号（从0开始）
     * @param supplier 数据供应函数
     */
    public <T> void writeRows(T[] dataArray, int startRow, RowDataSupplier<T> supplier) 
            throws IOException {
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
    
    /**
     * 结束Sheet流式写入
     */
    public void endSheet() throws IOException {
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
    
    int getSheetCount() {
        return sheetCount;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public void close() throws IOException {
        if (sheetCount > 0) {
            writeContainerStructure();
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
        
        // _rels/.rels
        container.addEntry("_rels/.rels", RelsGenerator.generateRootRels());
        
        // docProps/app.xml
        container.addEntry("docProps/app.xml", generateAppXml());
        
        // docProps/core.xml
        container.addEntry("docProps/core.xml", generateCoreXml());
        
        // xl/workbook.bin
        container.addEntry("xl/workbook.bin", workbookWriter.toBiff12Bytes());
        
        // xl/styles.bin (required)
        StylesWriter stylesWriter = new StylesWriter();
        container.addEntry("xl/styles.bin", stylesWriter.toBiff12Bytes());
        
        // xl/theme/theme1.xml
        container.addEntry("xl/theme/theme1.xml", generateThemeXml());
        
        // xl/_rels/workbook.bin.rels
        container.addEntry("xl/_rels/workbook.bin.rels", RelsGenerator.generateWorkbookRels(sheetCount, sharedStrings.getCount() > 0));
        
        // xl/sharedStrings.bin
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
        sb.append("<dc:creator>created by jxlsb</dc:creator>");
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