package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.CellDataSupplier;
import cn.itcraft.jxlsb.data.CellType;
import cn.itcraft.jxlsb.format.SheetParser;
import java.io.IOException;
import java.util.List;

/**
 * Worksheet.bin写入器
 * 
 * <p>支持两种写入模式：
 * <ul>
 *   <li>writeSheet：一次性写入全部数据</li>
 *   <li>流式追加：startStreaming + appendRows + finalizeStreaming</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class SheetWriter {
    
    private static final int MIN_RK_INTEGER = -536870912;
    private static final int MAX_RK_INTEGER = 536870911;
    private static final long EXCEL_EPOCH_MILLIS = -2208988800000L;
    private static final String DEFAULT_DATE_FORMAT = "m/d/yy h:mm";
    
    private static final byte[] ROW_HEIGHT_BYTES = {0x0E, 0x01};
    private static final byte[] ROW_FLAGS_BYTES = {0x00, 0x00, 0x00};
    private static final byte[] BOOL_TRUE = {1};
    private static final byte[] BOOL_FALSE = {0};
    
    private final SharedStringsTable sst;
    private final StylesWriter stylesWriter;
    private int defaultDateStyleId;
    
    private Biff12Writer streamingWriter;
    private int streamingColumnCount;
    private int streamingDimLastRowOffset;
    private SheetLimits streamingLimits;
    
    public SheetWriter(SharedStringsTable sst) {
        this.sst = sst;
        this.stylesWriter = null;
        this.defaultDateStyleId = 0;
    }
    
    public SheetWriter(SharedStringsTable sst, StylesWriter stylesWriter) {
        this.sst = sst;
        this.stylesWriter = stylesWriter;
        if (stylesWriter != null) {
            this.defaultDateStyleId = stylesWriter.addDateFormat(DEFAULT_DATE_FORMAT);
        } else {
            this.defaultDateStyleId = 0;
        }
    }
    
    public byte[] writeSheet(CellDataSupplier supplier, int rowCount, int columnCount) 
            throws IOException {
        // 维度限制在底层统一解析，批量/流式/模板填充共用同一策略。
        SheetLimits limits = SheetLimits.begin(columnCount);
        columnCount = limits.columns();
        rowCount = limits.rows(0, rowCount);

        // 预估缓冲区大小，设置上限避免超大Sheet过度分配（saturating 防溢出）
        int estimatedSize = (int) Math.min((long) rowCount * columnCount * 30 + 1024, 16L * 1024 * 1024);
        Biff12Writer w = new Biff12Writer(Math.max(estimatedSize, 4096));
        
        writeSheetHeader(w, rowCount, columnCount);
        
        w.writeEmptyRecord(Biff12RecordType.BrtBeginSheetData);
        
        for (int row = 0; row < rowCount; row++) {
            writeBrtRowHdr(w, row, columnCount);
            
            for (int col = 0; col < columnCount; col++) {
                CellData data = supplier.get(row, col);
                if (data != null && data.getType() != null && data.getType() != CellType.BLANK) {
                    writeCell(w, row, col, data);
                }
            }
        }
        
        writeSheetFooter(w);
        
        return w.toByteArray();
    }
    
    public void startStreaming(int columnCount) throws IOException {
        SheetLimits limits = SheetLimits.begin(columnCount);
        columnCount = limits.columns();

        int estimatedSize = 1024 * 1024; // 1MB initial buffer
        this.streamingWriter = new Biff12Writer(estimatedSize);
        this.streamingColumnCount = columnCount;
        this.streamingLimits = limits;

        // 前导先写维度（last_row 先置 0），结束时按偏移回填。
        this.streamingDimLastRowOffset = writeSheetHeader(streamingWriter, 0, columnCount);
        streamingWriter.writeEmptyRecord(Biff12RecordType.BrtBeginSheetData);
    }
    
    public void appendRows(CellDataSupplier supplier, int startRow, int batchSize, int columnCount) 
            throws IOException {
        if (streamingWriter == null || streamingLimits == null) {
            throw new IllegalStateException("Streaming not started, call startStreaming() first");
        }

        int rows = streamingLimits.rows(startRow, batchSize);
        int cols = streamingColumnCount;

        for (int row = startRow; row < startRow + rows; row++) {
            writeBrtRowHdr(streamingWriter, row, cols);
            
            for (int col = 0; col < cols; col++) {
                CellData data = supplier.get(row, col);
                if (data != null && data.getType() != null && data.getType() != CellType.BLANK) {
                    writeCell(streamingWriter, row, col, data);
                }
            }
        }
    }
    
    public byte[] finalizeStreaming(int totalRows, int columnCount) throws IOException {
        if (streamingWriter == null) {
            throw new IllegalStateException("Streaming not started");
        }

        // 回填维度 last_row，避免结束阶段整块拷贝行数据。
        int lastRow = totalRows > 0 ? totalRows - 1 : 0;
        streamingWriter.setIntLEAt(streamingDimLastRowOffset, lastRow);

        writeSheetFooter(streamingWriter);
        
        byte[] result = streamingWriter.toByteArray();
        streamingWriter = null;
        streamingColumnCount = 0;
        streamingLimits = null;
        
        return result;
    }
    
    /**
     * 写入 sheet 前导，返回 BrtWsDim 中 last_row 字段的字节偏移（用于回填）。
     */
    private int writeSheetHeader(Biff12Writer w, int rowCount, int columnCount) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtBeginSheet);
        
        writeBrtWsProp(w);

        int dimOffset = w.size();
        int lastRow = rowCount > 0 ? rowCount - 1 : 0;
        int lastCol = columnCount > 0 ? columnCount - 1 : 0;
        w.writeRecordHeader(Biff12RecordType.BrtWsDim, 16);
        w.writeIntLE(0);
        w.writeIntLE(lastRow);
        w.writeIntLE(0);
        w.writeIntLE(lastCol);
        
        writeViewRecords(w);

        // type varint(2) + varsize(1) + first_row(4)
        return dimOffset + 2 + 1 + 4;
    }
    
    private void writeSheetFooter(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtEndSheetData);
        
        writePageSetupRecords(w);
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndSheet);
    }
    
    /**
     * 写入BrtRowHdr记录
     * 正确结构：rw(4) + ixfe(4) + miyRw(2) + flags(3) + ccolspan(4) + rgBrtColspan(变量)
     */
    private void writeBrtRowHdr(Biff12Writer w, int row, int colCount) throws IOException {
        // 计算需要多少个 BrtColSpan
        int numSpans = 0;
        if (colCount > 0) {
            int lastSegment = (colCount - 1) / 1024;
            numSpans = lastSegment + 1;
        }
        
        // 记录大小: rw(4) + ixfe(4) + miyRw(2) + flags(3) + ccolspan(4) + BrtColSpan * numSpans
        int recordSize = 4 + 4 + 2 + 3 + 4 + (numSpans * 8);
        
        w.writeRecordHeader(Biff12RecordType.BrtRowHdr, recordSize);
        w.writeIntLE(row);      // rw
        w.writeIntLE(0);        // ixfe (style index, 0=default)
        w.writeBytes(ROW_HEIGHT_BYTES);  // miyRw = 270 (default row height in twips)
        w.writeBytes(ROW_FLAGS_BYTES);  // flags (3 bytes)
        
        // ccolspan
        w.writeIntLE(numSpans);
        
        // rgBrtColspan - 每个 BrtColSpan 指定一个段中列的范围
        for (int seg = 0; seg < numSpans; seg++) {
            int segStartCol = seg * 1024;
            int segEndCol = Math.min((seg + 1) * 1024 - 1, colCount - 1);
            w.writeIntLE(segStartCol);  // colMic
            w.writeIntLE(segEndCol);    // colLast
        }
    }
    
    /**
     * 写入单元格记录
     */
    private void writeCell(Biff12Writer w, int row, int col, CellData data) throws IOException {
        writeCellWithStyle(w, row, col, data, -1);
    }
    
    /**
     * 写入单元格记录（指定styleIndex）
     * 当styleIndex >= 0时使用指定值，否则根据data格式计算
     */
    private void writeCellWithStyle(Biff12Writer w, int row, int col, CellData data, int styleIndex) 
            throws IOException {
        int actualStyleIndex = styleIndex >= 0 ? styleIndex : 0;
        
        switch (data.getType()) {
            case NUMBER:
                double num = (Double) data.getValue();
                if (styleIndex < 0) {
                    actualStyleIndex = getStyleIdForFormat(data);
                }
                if (num == Math.floor(num) && num >= MIN_RK_INTEGER && num <= MAX_RK_INTEGER) {
                    writeBrtCellRk(w, row, col, (int) num, actualStyleIndex);
                } else {
                    writeBrtCellReal(w, row, col, num, actualStyleIndex);
                }
                break;
            case TEXT:
                int sstIdx = sst.addString((String) data.getValue());
                writeBrtCellIsst(w, row, col, sstIdx, actualStyleIndex);
                break;
            case DATE:
                long timestamp = (Long) data.getValue();
                double excelDate = toExcelDate(timestamp);
                if (styleIndex < 0) {
                    actualStyleIndex = getDateStyleIdForFormat(data);
                }
                writeBrtCellReal(w, row, col, excelDate, actualStyleIndex);
                break;
            case BOOLEAN:
                writeBrtCellBool(w, row, col, (Boolean) data.getValue(), actualStyleIndex);
                break;
            case BLANK:
                // 空白单元格不生成记录（与 rxlsb 一致），避免文件膨胀。
                return;
            default:
                throw new IllegalArgumentException("Unknown cell type: " + data.getType());
        }
    }
    
    private int getStyleIdForFormat(CellData data) {
        if (!data.hasFormatCode()) {
            return 0;
        }
        String formatCode = data.getFormatCode();
        return addFormatAndGetStyleId(formatCode);
    }
    
    private int getDateStyleIdForFormat(CellData data) {
        if (!data.hasFormatCode()) {
            return defaultDateStyleId;
        }
        String formatCode = data.getFormatCode();
        return addFormatAndGetStyleId(formatCode);
    }
    
    private int addFormatAndGetStyleId(String formatCode) {
        if (stylesWriter == null) {
            return 0;
        }
        return stylesWriter.addDateFormat(formatCode);
    }
    
    /**
     * 将Unix时间戳转换为Excel日期序列号
     * Excel日期从1900-01-01开始（序列号1.0）
     */
    private double toExcelDate(long timestamp) {
        double days = (timestamp - EXCEL_EPOCH_MILLIS) / (24.0 * 60 * 60 * 1000);
        return days + 1.0;
    }
    
    /**
     * 写入BrtCellRk记录（整数）
     * 结构：cell(8) + rk(4)
     * RK 编码：使用 IEEE 754 double 的高 32 位
     */
private void writeBrtCellRk(Biff12Writer w, int row, int col, int value, int styleIndex) 
            throws IOException {
        int recordSize = 8 + 4;  // cell + rk
        
        w.writeRecordHeader(Biff12RecordType.BrtCellRk, recordSize);
        w.writeCell(col, styleIndex);     // cell结构

        // 标准 RK 整数形式：num(30 位有符号)<<2 | fInt(bit1)=1
        int rk = (value << 2) | 0x02;
        w.writeIntLE(rk);
    }
    
    private void writeBrtCellReal(Biff12Writer w, int row, int col, double value, int styleIndex) 
            throws IOException {
        int recordSize = 8 + 8;  // cell + xnum
        
        w.writeRecordHeader(Biff12RecordType.BrtCellReal, recordSize);
        w.writeCell(col, styleIndex);     // cell结构
        w.writeDoubleLE(value);  // xnum
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
    private void writeBrtCellIsst(Biff12Writer w, int row, int col, int sstIndex, int styleIndex) 
            throws IOException {
        int recordSize = 8 + 4;  // cell + isst
        
        w.writeRecordHeader(Biff12RecordType.BrtCellIsst, recordSize);
        w.writeCell(col, styleIndex);     // cell结构
        w.writeIntLE(sstIndex);  // isst (SST index)
    }
    
    /**
     * 写入BrtCellBool记录（布尔值）
     * 结构：cell(8) + value(1)
     */
    private void writeBrtCellBool(Biff12Writer w, int row, int col, boolean value, int styleIndex) 
            throws IOException {
        int recordSize = 8 + 1;  // cell + value
        
        w.writeRecordHeader(Biff12RecordType.BrtCellBool, recordSize);
        w.writeCell(col, styleIndex);     // cell结构
        w.writeBytes(value ? BOOL_TRUE : BOOL_FALSE);
    }
    
    /**
     * 写入BrtCellBlank记录（空单元格）
     * 结构：cell(8)
     */
    private void writeBrtCellBlank(Biff12Writer w, int row, int col, int styleIndex) throws IOException {
        int recordSize = 8;  // cell only
        
        w.writeRecordHeader(Biff12RecordType.BrtCellBlank, recordSize);
        w.writeCell(col, styleIndex);  // cell结构
    }
    
    /**
     * 写入BrtWsProp记录（Sheet属性）
     * 从用户文件复制的实际数据
     */
    private void writeBrtWsProp(Biff12Writer w) throws IOException {
        byte[] data = {
            (byte)0xC9, 0x04, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00, 0x00, 0x00, 0x00
        };
        
        w.writeRecordHeader(Biff12RecordType.BrtWsProp, data.length);
        w.writeBytes(data);
    }
    
/**
     * 写入视图相关记录（从用户文件复制）
     */
    private void writeViewRecords(Biff12Writer w) throws IOException {
        // BrtBeginWsViews
        w.writeEmptyRecord(Biff12RecordType.BrtBeginWsViews);
        
        // BrtBeginWsView (30 bytes data)
        byte[] wsViewData = {
            (byte)0xDC, (byte)0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x40, 0x00, 0x00, 0x00, 0x64, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        w.writeRecordHeader(Biff12RecordType.BrtBeginWsView, wsViewData.length);
        w.writeBytes(wsViewData);
        
        // BrtSel (36 bytes data)
        byte[] selData = {
            0x03, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00,
            0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00,
            0x06, 0x00, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00,
            0x07, 0x00, 0x00, 0x00
        };
        w.writeRecordHeader(Biff12RecordType.BrtSel, selData.length);
        w.writeBytes(selData);
        
        // BrtEndWsView
        w.writeEmptyRecord(Biff12RecordType.BrtEndWsView);
        
        // BrtEndWsViews
        w.writeEmptyRecord(Biff12RecordType.BrtEndWsViews);
        
        // BrtWsFmtInfo (12 bytes data)
        byte[] fmtInfoData = {
            0x00, 0x09, 0x00, 0x00, 0x08, 0x00, 0x0E, 0x01,
            0x00, 0x00, 0x00, 0x00
        };
        w.writeRecordHeader(Biff12RecordType.BrtWsFmtInfo, fmtInfoData.length);
        w.writeBytes(fmtInfoData);
    }
    
    /**
     * 写入页面设置相关记录（从用户文件复制）
     */
    private void writePageSetupRecords(Biff12Writer w) throws IOException {
        // BrtDrawing (66 bytes data)
        byte[] drawingData = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00
        };
        w.writeRecordHeader(Biff12RecordType.BrtDrawing, drawingData.length);
        w.writeBytes(drawingData);
        
        // BrtPageSetupView (2 bytes data)
        byte[] psViewData = {0x10, 0x00};
        w.writeRecordHeader(Biff12RecordType.BrtPageSetupView, psViewData.length);
        w.writeBytes(psViewData);
        
        // BrtPageSetup (48 bytes data)
        byte[] psData = {
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xE8, 0x3F, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xE8, 0x3F, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xF0, 0x3F, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xF0, 0x3F, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xE0, 0x3F, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xE0, 0x3F
        };
        w.writeRecordHeader(Biff12RecordType.BrtPageSetup, psData.length);
        w.writeBytes(psData);
    }
    
    public int getStreamingColumnCount() {
        return streamingColumnCount;
    }
    
    public byte[] writeSheetWithTemplate(CellDataSupplier supplier, int rowCount, int columnCount,
                                          int startRow, int startCol, 
                                          List<SheetParser.CellInfo> templateCells,
                                          int templateMaxRow, int templateMaxCol,
                                          List<SheetParser.MergeCell> mergeCells) 
            throws IOException {
        // 底层维度限制：与批量/流式写入使用同一策略（模板填充也受控）。
        SheetLimits limits = SheetLimits.begin(Math.max(templateMaxCol + 1, startCol + columnCount));
        int totalCols = limits.columns();
        int maxCol = totalCols - 1;
        int totalRows = limits.rows(0, Math.max(templateMaxRow + 1, startRow + rowCount));
        
        // 预估缓冲区大小，设置上限避免超大Sheet过度分配（saturating 防溢出）
        int estimatedSize = (int) Math.min((long) totalRows * totalCols * 30 + 1024, 16L * 1024 * 1024);
        Biff12Writer w = new Biff12Writer(Math.max(estimatedSize, 4096));
        
        writeSheetHeader(w, totalRows, totalCols);
        w.writeEmptyRecord(Biff12RecordType.BrtBeginSheetData);
        
        for (int row = 0; row < totalRows; row++) {
            writeBrtRowHdr(w, row, totalCols);
            writeTemplateCellsForRow(w, row, templateCells, startRow, rowCount, startCol, columnCount, maxCol);
            writeFillCellsForRow(w, row, supplier, startRow, rowCount, startCol, columnCount, maxCol);
        }
        
        writeSheetFooterWithMerge(w, mergeCells);
        
        return w.toByteArray();
    }
    
    private void writeTemplateCellsForRow(Biff12Writer w, int row, 
                                          List<SheetParser.CellInfo> templateCells,
                                          int startRow, int rowCount, int startCol, int columnCount,
                                          int maxCol) 
            throws IOException {
        for (SheetParser.CellInfo cell : templateCells) {
            if (cell.col > maxCol) {
                continue;
            }
            if (cell.row == row && isOutsideFillArea(cell.row, cell.col, startRow, rowCount, startCol, columnCount)) {
                writeCellWithStyle(w, cell.row, cell.col, cell.data, cell.styleIndex);
            }
        }
    }
    
    private void writeFillCellsForRow(Biff12Writer w, int row, CellDataSupplier supplier,
                                       int startRow, int rowCount, int startCol, int columnCount,
                                       int maxCol) 
            throws IOException {
        if (row >= startRow && row < startRow + rowCount && startCol <= maxCol) {
            int fillEnd = Math.min(startCol + columnCount - 1, maxCol);
            for (int col = startCol; col <= fillEnd; col++) {
                CellData data = supplier.get(row, col);
                if (data != null && data.getType() != null && data.getType() != CellType.BLANK) {
                    writeCell(w, row, col, data);
                }
            }
        }
    }
    
    private boolean isOutsideFillArea(int cellRow, int cellCol, int startRow, int rowCount, int startCol, int columnCount) {
        return cellRow < startRow || cellRow >= startRow + rowCount ||
               cellCol < startCol || cellCol >= startCol + columnCount;
    }
    
    private void writeSheetFooterWithMerge(Biff12Writer w, List<SheetParser.MergeCell> mergeCells) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtEndSheetData);
        
        if (mergeCells != null && !mergeCells.isEmpty()) {
            writeMergeCells(w, mergeCells);
        }
        
        writePageSetupRecords(w);
        w.writeEmptyRecord(Biff12RecordType.BrtEndSheet);
    }
    
    private void writeMergeCells(Biff12Writer w, List<SheetParser.MergeCell> mergeCells) throws IOException {
        w.writeRecordHeader(Biff12RecordType.BrtBeginMergeCells, 4);
        w.writeIntLE(mergeCells.size());
        
        for (SheetParser.MergeCell mc : mergeCells) {
            w.writeRecordHeader(Biff12RecordType.BrtMergeCell, 16);
            w.writeIntLE(mc.rowFirst);
            w.writeIntLE(mc.rowLast);
            w.writeIntLE(mc.colFirst);
            w.writeIntLE(mc.colLast);
        }
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndMergeCells);
    }
}