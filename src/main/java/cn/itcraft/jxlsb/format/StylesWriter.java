package cn.itcraft.jxlsb.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Styles.bin写入器
 * 完整实现BIFF12样式表生成,不依赖硬编码模板
 */
public final class StylesWriter {
    
    private final CellStyleRegistry styleRegistry;
    private final List<String> addedDateFormats;
    
    public StylesWriter() {
        this.styleRegistry = new CellStyleRegistry();
        this.addedDateFormats = new ArrayList<>();
    }
    
    /**
     * 添加日期格式
     * @param dateFormat 日期格式字符串
     * @return 样式ID
     */
    public int addDateFormat(String dateFormat) {
        Objects.requireNonNull(dateFormat, "dateFormat不能为null");
        
        int styleId = styleRegistry.getDateStyleId(dateFormat);
        
        if (!addedDateFormats.contains(dateFormat)) {
            addedDateFormats.add(dateFormat);
        }
        
        return styleId;
    }
    
    /**
     * 生成styles.bin内容
     * MS-XLSB完整结构:
     * BrtBeginStyleSheet
     * BrtFmt (格式定义)
     * BrtFont (字体)
     * BrtFill (填充)
     * BrtBorder (边框)
     * BrtBeginCellStyleXFs + BrtCellStyleXF(默认) + BrtEndCellStyleXFs
     * BrtBeginCellXFs + BrtXF(单元格XF) + BrtEndCellXFs
     * BrtEndStyleSheet
     */
    public byte[] toBiff12Bytes() throws IOException {
        Biff12Writer w = new Biff12Writer(16 * 1024);
        
        w.writeEmptyRecord(Biff12RecordType.BrtBeginStyleSheet);
        
        writeNumberFormats(w);
        writeFonts(w);
        writeFills(w);
        writeBorders(w);
        
        writeCellStyleXFs(w);
        
        writeCellXFs(w);
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndStyleSheet);
        
        return w.toByteArray();
    }
    
    private void writeCellStyleXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtBeginCellStyleXFs);
        
        CellStyleFormat defaultStyle = CellStyleFormat.builder()
            .numFmtId(0)
            .fontId(0)
            .fillId(0)
            .borderId(0)
            .build();
        writeBrtXF(w, defaultStyle, 0);
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndCellStyleXFs);
    }
    
    private void writeCellXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtBeginCellXFs);
        
        List<CellStyleFormat> styles = styleRegistry.getStyles();
        for (int i = 0; i < styles.size(); i++) {
            writeBrtXF(w, styles.get(i), i);
        }
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndCellXFs);
    }
    
    private void writeNumberFormats(Biff12Writer w) throws IOException {
        Map<Integer, String> customFormats = styleRegistry.getFormatRegistry().getCustomFormats();
        
        for (Map.Entry<Integer, String> entry : customFormats.entrySet()) {
            writeBrtFmt(w, entry.getKey(), entry.getValue());
        }
    }
    
    private void writeBrtFmt(Biff12Writer w, int formatId, String formatString) throws IOException {
        w.writeRecordHeader(Biff12RecordType.BrtFmt, calculateFmtSize(formatString));
        w.writeIntLE(formatId);
        w.writeXLWideString(formatString);
    }
    
    private int calculateFmtSize(String formatString) {
        return 2 + (formatString.length() * 2 + 4);
    }
    
    private void writeFonts(Biff12Writer w) throws IOException {
        byte[] defaultFontData = buildDefaultFont();
        w.writeRecordHeader(Biff12RecordType.BrtFont, defaultFontData.length);
        w.writeBytes(defaultFontData);
    }
    
    private byte[] buildDefaultFont() {
        byte[] data = new byte[134];
        
        data[0] = 0x0E;
        data[1] = 0x01;
        
        data[4] = (byte)(11 * 20);
        data[5] = (byte)((11 * 20) >> 8);
        
        String fontName = "Calibri";
        byte[] nameBytes = fontName.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        data[6] = (byte)fontName.length();
        data[7] = (byte)(fontName.length() >> 8);
        System.arraycopy(nameBytes, 0, data, 8, nameBytes.length);
        
        return data;
    }
    
    private void writeFills(Biff12Writer w) throws IOException {
        writeBrtFillEmpty(w);
        writeBrtFillGray125(w);
    }
    
    private void writeBrtFillEmpty(Biff12Writer w) throws IOException {
        byte[] data = new byte[4];
        w.writeRecordHeader(Biff12RecordType.BrtFill, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtFillGray125(Biff12Writer w) throws IOException {
        byte[] data = {(byte)0x02, 0x00, (byte)0x80, 0x00};
        w.writeRecordHeader(Biff12RecordType.BrtFill, data.length);
        w.writeBytes(data);
    }
    
    private void writeBorders(Biff12Writer w) throws IOException {
        byte[] data = new byte[24];
        w.writeRecordHeader(Biff12RecordType.BrtBorder, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtXF(Biff12Writer w, CellStyleFormat style, int xfId) throws IOException {
        byte[] data = new byte[20];
        
        data[0] = (byte)style.getNumFmtId();
        data[1] = (byte)(style.getNumFmtId() >> 8);
        
        data[2] = (byte)style.getFontId();
        data[3] = (byte)(style.getFontId() >> 8);
        
        data[4] = (byte)style.getFillId();
        data[5] = (byte)(style.getFillId() >> 8);
        
        data[6] = (byte)style.getBorderId();
        data[7] = (byte)(style.getBorderId() >> 8);
        
        w.writeRecordHeader(Biff12RecordType.BrtXF, data.length);
        w.writeBytes(data);
    }
    
    CellStyleRegistry getStyleRegistry() {
        return styleRegistry;
    }
}