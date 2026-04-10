package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.format.record.*;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.data.CellType;
import java.io.IOException;

/**
 * XLSB格式写入器
 * 
 * <p>将数据结构写入XLSB二进制格式。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbFormatWriter {
    
    private final OffHeapAllocator allocator;
    private final RecordWriter writer;
    
    public XlsbFormatWriter(RecordWriter writer) {
        this.allocator = AllocatorFactory.createDefaultAllocator();
        this.writer = writer;
    }
    
    public void writeWorkbookStart() throws IOException {
        BeginBookRecord beginBook = new BeginBookRecord();
        beginBook.writeTo(writer);
        
        MemoryBlock versionBlock = allocator.allocate(64);
        VersionRecord version = VersionRecord.create(VersionRecord.VERSION_2016, versionBlock);
        version.writeTo(writer);
        version.close();
    }
    
    public void writeWorkbookEnd() throws IOException {
        EndBookRecord endBook = new EndBookRecord();
        endBook.writeTo(writer);
    }
    
    public void writeSheetStart(int sheetIndex) throws IOException {
        MemoryBlock sheetBlock = allocator.allocate(64);
        BeginSheetRecord beginSheet = BeginSheetRecord.create(sheetIndex, sheetBlock);
        beginSheet.writeTo(writer);
        beginSheet.close();
    }
    
    public void writeSheetEnd() throws IOException {
        EndSheetRecord endSheet = new EndSheetRecord();
        endSheet.writeTo(writer);
    }
    
    public void writeRowStart(int rowIndex, int columnCount) throws IOException {
        MemoryBlock rowBlock = allocator.allocate(64);
        BeginRowRecord beginRow = BeginRowRecord.create(rowIndex, columnCount, rowBlock);
        beginRow.writeTo(writer);
        beginRow.close();
    }
    
    public void writeRowEnd() throws IOException {
        EndRowRecord endRow = new EndRowRecord();
        endRow.writeTo(writer);
    }
    
    public void writeCell(OffHeapCell cell) throws IOException {
        MemoryBlock cellBlock = allocator.allocate(2048);
        
        CellType cellType = cell.getType();
        Object value = null;
        
        switch (cellType) {
            case NUMBER:
                value = cell.getNumber();
                break;
            case DATE:
                value = cell.getDate();
                break;
            case BOOLEAN:
                value = cell.getBoolean();
                break;
            case TEXT:
                value = cell.getText();
                break;
            default:
                value = "";
                break;
        }
        
        CellRecord cellRecord = CellRecord.create(
            cell.getRowIndex(), 
            cell.getColIndex(), 
            cellType, 
            value, 
            cellBlock
        );
        
        cellRecord.writeTo(writer);
        cellRecord.close();
    }
    
    public void writeCellDirect(int rowIndex, int colIndex, CellType cellType, Object value) throws IOException {
        int estimatedSize = estimateCellSize(cellType, value);
        MemoryBlock cellBlock = allocator.allocate(estimatedSize);
        
        CellRecord cellRecord = CellRecord.create(rowIndex, colIndex, cellType, value, cellBlock);
        cellRecord.writeTo(writer);
        cellRecord.close();
    }
    
    private int estimateCellSize(CellType cellType, Object value) {
        switch (cellType) {
            case NUMBER:
                return 64;
            case DATE:
                return 64;
            case BOOLEAN:
                return 64;
            case TEXT:
                String text = (String) value;
                return 64 + (text != null ? Math.min(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, 1000) + 4 : 0);
            default:
                return 64;
        }
    }
    
    public void writeSheet(OffHeapSheet sheet) throws IOException {
        writeSheetStart(sheet.getSheetIndex());
        
        for (int rowIdx = 0; rowIdx < sheet.getRowCount(); rowIdx++) {
            OffHeapRow row = sheet.createRow(rowIdx);
            
            writeRowStart(rowIdx, row.getColumnCount());
            
            for (int colIdx = 0; colIdx < row.getColumnCount(); colIdx++) {
                OffHeapCell cell = row.getCell(colIdx);
                writeCell(cell);
            }
            
            writeRowEnd();
            row.close();
        }
        
        writeSheetEnd();
    }
    
    public void close() {
    }
}