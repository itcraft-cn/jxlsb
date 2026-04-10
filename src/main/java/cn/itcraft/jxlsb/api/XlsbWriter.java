package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.XlsbContainer;
import cn.itcraft.jxlsb.container.ContentTypes;
import cn.itcraft.jxlsb.container.RelsGenerator;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.format.WorkbookWriter;
import cn.itcraft.jxlsb.format.Biff12Constants;
import java.nio.file.Path;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Objects;

/**
 * XLSB文件写入器
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbWriter implements AutoCloseable {
    
    private final XlsbContainer container;
    private final SharedStringsTable sharedStrings;
    private final WorkbookWriter workbookWriter;
    private int sheetCount = 0;
    
    private XlsbWriter(Builder builder) throws IOException {
        Objects.requireNonNull(builder.path, "Path must not be null");
        this.container = XlsbContainer.create(builder.path);
        this.sharedStrings = new SharedStringsTable();
        this.workbookWriter = new WorkbookWriter();
    }
    
    public void writeBatch(String sheetName, CellDataSupplier supplier,
                           int rowCount, int columnCount) throws IOException {
        workbookWriter.addSheet(sheetName);
        
        byte[] sheetData = writeSheetData(supplier, rowCount, columnCount);
        container.addEntry("xl/worksheets/sheet" + (sheetCount + 1) + ".bin", sheetData);
        sheetCount++;
    }
    
    private byte[] writeSheetData(CellDataSupplier supplier, int rowCount, int columnCount) 
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        // BEGIN_SHEET
        writeRecordHeader(dos, Biff12Constants.BEGIN_SHEET, 0);
        
        // Dimension
        writeRecordHeader(dos, Biff12Constants.WS_DIMENSION, 16);
        writeIntLE(dos, 0);
        writeIntLE(dos, rowCount - 1);
        writeIntLE(dos, 0);
        writeIntLE(dos, columnCount - 1);
        
        // BEGIN_SHEET_DATA
        writeRecordHeader(dos, Biff12Constants.BEGIN_SHEET_DATA, 0);
        
        // Sheet data
        for (int row = 0; row < rowCount; row++) {
            // ROW record
            writeRecordHeader(dos, Biff12Constants.ROW, 16);
            writeIntLE(dos, row);
            writeIntLE(dos, row);
            writeIntLE(dos, columnCount > 0 ? columnCount - 1 : 0);
            writeIntLE(dos, 0);
            
            for (int col = 0; col < columnCount; col++) {
                CellData data = supplier.get(row, col);
                if (data != null && data.getType() != null) {
                    writeCellRecord(dos, row, col, data);
                }
            }
        }
        
        // END_SHEET_DATA
        writeRecordHeader(dos, Biff12Constants.END_SHEET_DATA, 0);
        
        // END_SHEET
        writeRecordHeader(dos, Biff12Constants.END_SHEET, 0);
        
        dos.flush();
        return baos.toByteArray();
    }
    
    private void writeCellRecord(DataOutputStream dos, int row, int col, CellData data) 
            throws IOException {
        switch (data.getType()) {
            case NUMBER:
                double num = (Double) data.getValue();
                writeRecordHeader(dos, Biff12Constants.CELL_FLOAT, 16);
                writeIntLE(dos, row);
                writeIntLE(dos, col);
                writeDoubleLE(dos, num);
                break;
            case TEXT:
                String text = (String) data.getValue();
                int sstIdx = sharedStrings.addString(text);
                writeRecordHeader(dos, Biff12Constants.CELL_STRING, 12);
                writeIntLE(dos, row);
                writeIntLE(dos, col);
                writeIntLE(dos, sstIdx);
                break;
            case BOOLEAN:
                boolean bool = (Boolean) data.getValue();
                writeRecordHeader(dos, Biff12Constants.CELL_BOOL, 9);
                writeIntLE(dos, row);
                writeIntLE(dos, col);
                dos.writeByte(bool ? 1 : 0);
                break;
            case BLANK:
                writeRecordHeader(dos, Biff12Constants.CELL_BLANK, 8);
                writeIntLE(dos, row);
                writeIntLE(dos, col);
                break;
            default:
                break;
        }
    }
    
    private void writeRecordHeader(DataOutputStream dos, int type, int size) throws IOException {
        dos.write(type & 0xFF);
        dos.write((type >> 8) & 0xFF);
        dos.write(size & 0xFF);
        dos.write((size >> 8) & 0xFF);
    }
    
    private void writeIntLE(DataOutputStream dos, int value) throws IOException {
        dos.write(value & 0xFF);
        dos.write((value >> 8) & 0xFF);
        dos.write((value >> 16) & 0xFF);
        dos.write((value >> 24) & 0xFF);
    }
    
    private void writeDoubleLE(DataOutputStream dos, double value) throws IOException {
        long bits = Double.doubleToLongBits(value);
        dos.write((int)(bits & 0xFF));
        dos.write((int)((bits >> 8) & 0xFF));
        dos.write((int)((bits >> 16) & 0xFF));
        dos.write((int)((bits >> 24) & 0xFF));
        dos.write((int)((bits >> 32) & 0xFF));
        dos.write((int)((bits >> 40) & 0xFF));
        dos.write((int)((bits >> 48) & 0xFF));
        dos.write((int)((bits >> 56) & 0xFF));
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