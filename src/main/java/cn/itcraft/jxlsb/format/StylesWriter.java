package cn.itcraft.jxlsb.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StylesWriter {
    
    private final CellStyleRegistry styleRegistry;
    private final List<String> addedDateFormats;
    
    public StylesWriter() {
        this.styleRegistry = new CellStyleRegistry();
        this.addedDateFormats = new ArrayList<>();
    }
    
    public int addDateFormat(String dateFormat) {
        Objects.requireNonNull(dateFormat, "dateFormat不能为null");
        int styleId = styleRegistry.getDateStyleId(dateFormat);
        if (!addedDateFormats.contains(dateFormat)) {
            addedDateFormats.add(dateFormat);
        }
        return styleId;
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        Biff12Writer w = new Biff12Writer(16 * 1024);
        
        w.writeEmptyRecord(Biff12RecordType.BrtBeginStyleSheet);
        
        writeBrtBeginFmts(w);
        writeNumberFormats(w);
        writeBrtEndFmts(w);
        
        writeBrtBeginFonts(w);
        writeFonts(w);
        writeBrtEndFonts(w);
        
        writeBrtBeginFills(w);
        writeFills(w);
        writeBrtEndFills(w);
        
        writeBrtBeginBorders(w);
        writeBorders(w);
        writeBrtEndBorders(w);
        
        writeBrtBeginCellStyleXFs(w);
        writeBrtCellStyleXF(w, 0);
        writeBrtEndCellStyleXFs(w);
        
        writeBrtBeginCellXFs(w);
        writeCellXFs(w);
        writeBrtEndCellXFs(w);
        
        writeBrtBeginStyles(w);
        writeBrtEndStyles(w);
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndStyleSheet);
        
        return w.toByteArray();
    }
    
    private void writeBrtBeginFmts(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(615);
    }
    
    private void writeBrtEndFmts(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(616);
    }
    
    private void writeBrtBeginFonts(Biff12Writer w) throws IOException {
        byte[] data = new byte[4];
        data[0] = 1;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;
        w.writeRecordHeader(611, 4);
        w.writeBytes(data);
    }
    
    private void writeBrtEndFonts(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(612);
    }
    
    private void writeBrtBeginFills(Biff12Writer w) throws IOException {
        byte[] data = new byte[4];
        data[0] = 2;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;
        w.writeRecordHeader(603, 4);
        w.writeBytes(data);
    }
    
    private void writeBrtEndFills(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(604);
    }
    
    private void writeBrtBeginBorders(Biff12Writer w) throws IOException {
        byte[] data = new byte[4];
        data[0] = 1;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;
        w.writeRecordHeader(613, 4);
        w.writeBytes(data);
    }
    
    private void writeBrtEndBorders(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(614);
    }
    
    private void writeBrtBeginCellStyleXFs(Biff12Writer w) throws IOException {
        byte[] data = new byte[4];
        data[0] = 1;
        data[1] = 0;
        data[2] = 0;
        data[3] = 0;
        w.writeRecordHeader(Biff12RecordType.BrtBeginCellStyleXFs, 4);
        w.writeBytes(data);
    }
    
    private void writeBrtEndCellStyleXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtEndCellStyleXFs);
    }
    
    private void writeBrtBeginCellXFs(Biff12Writer w) throws IOException {
        int count = styleRegistry.getStyles().size();
        byte[] data = new byte[4];
        data[0] = (byte)count;
        data[1] = (byte)(count >> 8);
        data[2] = (byte)(count >> 16);
        data[3] = (byte)(count >> 24);
        w.writeRecordHeader(Biff12RecordType.BrtBeginCellXFs, 4);
        w.writeBytes(data);
    }
    
    private void writeBrtEndCellXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(Biff12RecordType.BrtEndCellXFs);
    }
    
    private void writeBrtBeginStyles(Biff12Writer w) throws IOException {
        byte[] data = new byte[4];
        w.writeRecordHeader(617, 4);
        w.writeBytes(data);
    }
    
    private void writeBrtEndStyles(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(618);
    }
    
    private void writeNumberFormats(Biff12Writer w) throws IOException {
        Map<Integer, String> customFormats = styleRegistry.getFormatRegistry().getCustomFormats();
        for (Map.Entry<Integer, String> entry : customFormats.entrySet()) {
            writeBrtFmt(w, entry.getKey(), entry.getValue());
        }
    }
    
    private void writeBrtFmt(Biff12Writer w, int formatId, String formatString) throws IOException {
        byte[] strBytes = formatString.getBytes(StandardCharsets.UTF_16LE);
        int strLen = formatString.length();
        int recordSize = 2 + 4 + strLen * 2;
        
        w.writeRecordHeader(Biff12RecordType.BrtFmt, recordSize);
        w.writeShortLE(formatId);
        w.writeIntLE(strLen);
        w.writeBytes(strBytes);
    }
    
    private void writeFonts(Biff12Writer w) throws IOException {
        byte[] fontData = buildDefaultFont();
        w.writeRecordHeader(Biff12RecordType.BrtFont, fontData.length);
        w.writeBytes(fontData);
    }
    
    private byte[] buildDefaultFont() {
        byte[] data = new byte[134];
        data[0] = 0x0E;
        data[1] = 0x01;
        data[4] = (byte)(11 * 20);
        data[5] = (byte)((11 * 20) >> 8);
        String fontName = "Calibri";
        byte[] nameBytes = fontName.getBytes(StandardCharsets.UTF_16LE);
        data[6] = (byte)fontName.length();
        data[7] = (byte)(fontName.length() >> 8);
        System.arraycopy(nameBytes, 0, data, 8, nameBytes.length);
        return data;
    }
    
    private void writeFills(Biff12Writer w) throws IOException {
        writeBrtFill(w, new byte[4]);
        writeBrtFill(w, new byte[]{0x02, 0x00, (byte)0x80, 0x00});
    }
    
    private void writeBrtFill(Biff12Writer w, byte[] data) throws IOException {
        w.writeRecordHeader(Biff12RecordType.BrtFill, data.length);
        w.writeBytes(data);
    }
    
    private void writeBorders(Biff12Writer w) throws IOException {
        byte[] data = new byte[24];
        w.writeRecordHeader(Biff12RecordType.BrtBorder, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtCellStyleXF(Biff12Writer w, int xfId) throws IOException {
        byte[] data = new byte[24];
        data[8] = (byte)xfId;
        data[9] = (byte)(xfId >> 8);
        w.writeRecordHeader(Biff12RecordType.BrtXF, data.length);
        w.writeBytes(data);
    }
    
    private void writeCellXFs(Biff12Writer w) throws IOException {
        List<CellStyleFormat> styles = styleRegistry.getStyles();
        for (int i = 0; i < styles.size(); i++) {
            writeBrtXF(w, styles.get(i), i);
        }
    }
    
    private void writeBrtXF(Biff12Writer w, CellStyleFormat style, int xfId) throws IOException {
        byte[] data = new byte[24];
        
        data[0] = (byte)style.getNumFmtId();
        data[1] = (byte)(style.getNumFmtId() >> 8);
        
        data[2] = (byte)style.getFontId();
        data[3] = (byte)(style.getFontId() >> 8);
        
        data[4] = (byte)style.getFillId();
        data[5] = (byte)(style.getFillId() >> 8);
        
        data[6] = (byte)style.getBorderId();
        data[7] = (byte)(style.getBorderId() >> 8);
        
        data[8] = (byte)xfId;
        data[9] = (byte)(xfId >> 8);
        data[10] = (byte)(xfId >> 16);
        data[11] = (byte)(xfId >> 24);
        
        w.writeRecordHeader(Biff12RecordType.BrtXF, data.length);
        w.writeBytes(data);
    }
    
    CellStyleRegistry getStyleRegistry() {
        return styleRegistry;
    }
}