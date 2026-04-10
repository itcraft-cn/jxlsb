package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.CellDataSupplier;
import java.io.IOException;

/**
 * Worksheet.bin写入器
 * 严格按照MS-XLSB规范实现
 */
public final class SheetWriter {
    
    private final SharedStringsTable sst;
    
    public SheetWriter(SharedStringsTable sst) {
        this.sst = sst;
    }
    
    /**
     * 生成worksheet.bin内容
     * 记录序列：BrtBeginSheet -> BrtWsDim -> BrtBeginSheetData -> ... -> BrtEndSheet
     */
    public byte[] writeSheet(CellDataSupplier supplier, int rowCount, int columnCount) 
            throws IOException {
        Biff12Writer w = new Biff12Writer();
        
        // BrtBeginSheet
        w.writeEmptyRecord(Biff12RecordType.BrtBeginSheet);
        
        // BrtWsDim - Sheet维度
        // 结构：rwFirst(4) + rwLast(4) + colFirst(4) + colLast(4)
        if (rowCount > 0 && columnCount > 0) {
            w.writeRecordHeader(Biff12RecordType.BrtWsDim, 16);
            w.writeIntLE(0);           // rwFirst
            w.writeIntLE(rowCount - 1); // rwLast
            w.writeIntLE(0);           // colFirst
            w.writeIntLE(columnCount - 1); // colLast
        }
        
        // BrtBeginSheetData
        w.writeEmptyRecord(Biff12RecordType.BrtBeginSheetData);
        
        // 行和单元格数据
        for (int row = 0; row < rowCount; row++) {
            // BrtRowHdr
            // 结构：rw(4) + ixfe(4) + miyRw(2) + flags(4) + ccolspan(4) + ...
            writeBrtRowHdr(w, row, columnCount);
            
            // 单元格记录
            for (int col = 0; col < columnCount; col++) {
                CellData data = supplier.get(row, col);
                if (data != null && data.getType() != null) {
                    writeCell(w, row, col, data);
                }
            }
        }
        
        // BrtEndSheetData
        w.writeEmptyRecord(Biff12RecordType.BrtEndSheetData);
        
        // BrtEndSheet
        w.writeEmptyRecord(Biff12RecordType.BrtEndSheet);
        
        return w.toByteArray();
    }
    
    /**
     * 写入BrtRowHdr记录
     * 简化结构：rw(4) + ixfe(4) + miyRw(2) + flags(4) + ccolspan(4)
     */
    private void writeBrtRowHdr(Biff12Writer w, int row, int colCount) throws IOException {
        int recordSize = 4 + 4 + 2 + 4 + 4;  // 18 bytes minimum
        
        w.writeRecordHeader(Biff12RecordType.BrtRowHdr, recordSize);
        w.writeIntLE(row);      // rw
        w.writeIntLE(0);        // ixfe (style index, 0=default)
        w.writeBytes(new byte[]{0, 0});  // miyRw (row height, 0=default)
        w.writeIntLE(0);        // flags
        w.writeIntLE(0);        // ccolspan (column span count)
    }
    
    /**
     * 写入单元格记录
     */
    private void writeCell(Biff12Writer w, int row, int col, CellData data) throws IOException {
        switch (data.getType()) {
            case NUMBER:
                writeBrtCellReal(w, row, col, (Double) data.getValue());
                break;
            case TEXT:
                int sstIdx = sst.addString((String) data.getValue());
                writeBrtCellIsst(w, row, col, sstIdx);
                break;
            case BOOLEAN:
                writeBrtCellBool(w, row, col, (Boolean) data.getValue());
                break;
            case BLANK:
                writeBrtCellBlank(w, row, col);
                break;
            default:
                break;
        }
    }
    
    /**
     * 写入BrtCellReal记录（浮点数）
     * 结构：cell(8) + xnum(8)
     */
    private void writeBrtCellReal(Biff12Writer w, int row, int col, double value) 
            throws IOException {
        int recordSize = 8 + 8;  // cell + xnum
        
        w.writeRecordHeader(Biff12RecordType.BrtCellReal, recordSize);
        w.writeCell(col, 0);     // cell结构
        w.writeDoubleLE(value);  // xnum
    }
    
    /**
     * 写入BrtCellIsst记录（字符串索引）
     * 结构：cell(8) + isst(4)
     */
    private void writeBrtCellIsst(Biff12Writer w, int row, int col, int sstIndex) 
            throws IOException {
        int recordSize = 8 + 4;  // cell + isst
        
        w.writeRecordHeader(Biff12RecordType.BrtCellIsst, recordSize);
        w.writeCell(col, 0);     // cell结构
        w.writeIntLE(sstIndex);  // isst (SST index)
    }
    
    /**
     * 写入BrtCellBool记录（布尔值）
     * 结构：cell(8) + value(1)
     */
    private void writeBrtCellBool(Biff12Writer w, int row, int col, boolean value) 
            throws IOException {
        int recordSize = 8 + 1;  // cell + value
        
        w.writeRecordHeader(Biff12RecordType.BrtCellBool, recordSize);
        w.writeCell(col, 0);     // cell结构
        w.writeBytes(new byte[]{(byte)(value ? 1 : 0)});
    }
    
    /**
     * 写入BrtCellBlank记录（空单元格）
     * 结构：cell(8)
     */
    private void writeBrtCellBlank(Biff12Writer w, int row, int col) throws IOException {
        int recordSize = 8;  // cell only
        
        w.writeRecordHeader(Biff12RecordType.BrtCellBlank, recordSize);
        w.writeCell(col, 0);  // cell结构
    }
}