package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.XlsbContainer;
import cn.itcraft.jxlsb.container.ContentTypes;
import cn.itcraft.jxlsb.container.RelsGenerator;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.format.WorkbookWriter;
import cn.itcraft.jxlsb.format.SheetWriter;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Objects;

/**
 * XLSB文件写入器
 * 严格按照MS-XLSB规范实现
 */
public final class XlsbWriter implements AutoCloseable {
    
    private final XlsbContainer container;
    private final SharedStringsTable sharedStrings;
    private final WorkbookWriter workbookWriter;
    private final SheetWriter sheetWriter;
    private int sheetCount = 0;
    
    private XlsbWriter(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.container = XlsbContainer.create(builder.path);
        this.sharedStrings = new SharedStringsTable();
        this.workbookWriter = new WorkbookWriter();
        this.sheetWriter = new SheetWriter(sharedStrings);
    }
    
    /**
     * 批量写入Sheet数据
     */
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                           int rowCount, int columnCount) throws IOException {
        workbookWriter.addSheet(sheetName);
        
        byte[] sheetData = sheetWriter.writeSheet(supplier, rowCount, columnCount);
        container.addEntry("xl/worksheets/sheet" + (sheetCount + 1) + ".bin", sheetData);
        sheetCount++;
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
        // [Content_Types].xml
        ContentTypes ct = new ContentTypes();
        ct.addOverride("/xl/workbook.bin", "application/vnd.ms-excel.sheet.binary.macroEnabled.main");
        for (int i = 1; i <= sheetCount; i++) {
            ct.addOverride("/xl/worksheets/sheet" + i + ".bin", 
                          "application/vnd.ms-excel.worksheet");
        }
        if (sharedStrings.getCount() > 0) {
            ct.addOverride("/xl/sharedStrings.bin", "application/vnd.ms-excel.sharedStrings");
        }
        container.addEntry("[Content_Types].xml", ct.toXml());
        
        // _rels/.rels
        container.addEntry("_rels/.rels", RelsGenerator.generateRootRels());
        
        // xl/workbook.bin
        container.addEntry("xl/workbook.bin", workbookWriter.toBiff12Bytes());
        
        // xl/_rels/workbook.bin.rels
        container.addEntry("xl/_rels/workbook.bin.rels", RelsGenerator.generateWorkbookRels(sheetCount));
        
        // xl/sharedStrings.bin
        if (sharedStrings.getCount() > 0) {
            container.addEntry("xl/sharedStrings.bin", sharedStrings.toBiff12Bytes());
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