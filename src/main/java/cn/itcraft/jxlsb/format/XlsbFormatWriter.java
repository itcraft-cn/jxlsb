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
    
    public void writeWorkbookStart() {
        BeginBookRecord beginBook = new BeginBookRecord();
        beginBook.writeTo(writer);
        
        MemoryBlock versionBlock = allocator.allocate(64);
        VersionRecord version = VersionRecord.create(VersionRecord.VERSION_2016, versionBlock);
        version.writeTo(writer);
        version.close();
    }
    
    public void writeWorkbookEnd() {
        EndBookRecord endBook = new EndBookRecord();
        endBook.writeTo(writer);
    }
    
    public void writeSheetStart(int sheetIndex) {
        MemoryBlock sheetBlock = allocator.allocate(64);
        BeginSheetRecord beginSheet = BeginSheetRecord.create(sheetIndex, sheetBlock);
        beginSheet.writeTo(writer);
        beginSheet.close();
    }
    
    public void writeSheetEnd() {
        EndSheetRecord endSheet = new EndSheetRecord();
        endSheet.writeTo(writer);
    }
    
    public void writeRowStart(int rowIndex, int columnCount) {
        MemoryBlock rowBlock = allocator.allocate(64);
        BeginRowRecord beginRow = BeginRowRecord.create(rowIndex, columnCount, rowBlock);
        beginRow.writeTo(writer);
        beginRow.close();
    }
    
    public void writeRowEnd() {
        EndRowRecord endRow = new EndRowRecord();
        endRow.writeTo(writer);
    }
    
    public void writeCell(OffHeapCell cell) {
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