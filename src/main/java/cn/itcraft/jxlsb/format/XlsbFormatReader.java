package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.format.record.*;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.data.CellType;
import cn.itcraft.jxlsb.api.SheetHandler;
import cn.itcraft.jxlsb.api.CellHandler;
import java.io.IOException;

/**
 * XLSB格式读取器
 * 
 * <p>从XLSB二进制格式读取数据。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class XlsbFormatReader {
    
    private final OffHeapAllocator allocator;
    private final RecordParser parser;
    
    public XlsbFormatReader() {
        this.allocator = AllocatorFactory.createDefaultAllocator();
        this.parser = new RecordParser();
    }
    
    public void readSheet(MemoryBlock dataBlock, SheetHandler handler) 
        throws IOException {
        SheetContext context = new SheetContext();
        
        parser.parseStream(dataBlock, record -> {
            if (record instanceof BeginSheetRecord) {
                BeginSheetRecord beginSheet = (BeginSheetRecord) record;
                context.sheetIndex = beginSheet.getSheetIndex();
            } else if (record instanceof BeginRowRecord) {
                BeginRowRecord beginRow = (BeginRowRecord) record;
                context.rowIndex = beginRow.getRowIndex();
                context.columnCount = beginRow.getColumnCount();
            } else if (record instanceof CellRecord) {
                CellRecord cellRecord = (CellRecord) record;
                context.cellRecords.add(cellRecord);
            } else if (record instanceof EndSheetRecord) {
                OffHeapSheet sheet = buildSheet(context);
                handler.handle(sheet);
                sheet.close();
                context.reset();
            }
        });
    }
    
    private OffHeapSheet buildSheet(SheetContext context) {
        OffHeapSheet sheet = new OffHeapSheet(
            "Sheet" + context.sheetIndex,
            context.sheetIndex,
            context.rowIndex + 1,
            context.columnCount
        );
        
        for (CellRecord cellRecord : context.cellRecords) {
            OffHeapRow row = sheet.createRow(cellRecord.getRowIndex());
            OffHeapCell cell = row.getCell(cellRecord.getColIndex());
            
            CellType cellType = cellRecord.getCellType();
            switch (cellType) {
                case NUMBER:
                    cell.setNumber(cellRecord.getNumberValue());
                    break;
                case DATE:
                    cell.setDate(cellRecord.getDateValue());
                    break;
                case BOOLEAN:
                    cell.setBoolean(cellRecord.getBooleanValue());
                    break;
                case TEXT:
                    cell.setText(cellRecord.getTextValue());
                    break;
                default:
                    break;
            }
        }
        
        return sheet;
    }
    
    private static final class SheetContext {
        int sheetIndex = 0;
        int rowIndex = 0;
        int columnCount = 0;
        java.util.List<CellRecord> cellRecords = new java.util.ArrayList<>();
        
        void reset() {
            sheetIndex = 0;
            rowIndex = 0;
            columnCount = 0;
            cellRecords.clear();
        }
    }
}