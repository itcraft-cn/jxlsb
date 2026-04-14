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
        
        writeBrtBeginCellStyleXFs(w);
        writeFormats(w);
        writeFonts(w);
        writeFills(w);
        writeBorders(w);
        writeCellStyleXFs(w);
        writeBrtEndCellStyleXFs(w);
        
        writeBrtBeginCellXFs(w);
        writeCellXFs(w);
        writeBrtEndCellXFs(w);
        
        writeBrtBeginStyles(w);
        writeBrtEndStyles(w);
        
        return w.toByteArray();
    }
    
    private void writeBrtBeginCellStyleXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(278);
    }
    
    private void writeBrtEndCellStyleXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(279);
    }
    
    private void writeBrtBeginCellXFs(Biff12Writer w) throws IOException {
        int count = styleRegistry.getStyles().size();
        w.writeRecordHeader(280, 4);
        w.writeIntLE(count);
    }
    
    private void writeBrtEndCellXFs(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(281);
    }
    
    private void writeBrtBeginStyles(Biff12Writer w) throws IOException {
        w.writeRecordHeader(617, 4);
        w.writeIntLE(0);
    }
    
    private void writeBrtEndStyles(Biff12Writer w) throws IOException {
        w.writeEmptyRecord(618);
    }
    
    private void writeFormats(Biff12Writer w) throws IOException {
        Map<Integer, String> customFormats = styleRegistry.getFormatRegistry().getCustomFormats();
        
        w.writeRecordHeader(615, 4);
        w.writeIntLE(customFormats.size());
        
        for (Map.Entry<Integer, String> entry : customFormats.entrySet()) {
            writeBrtFmt(w, entry.getKey(), entry.getValue());
        }
        
        w.writeEmptyRecord(616);
    }
    
    private void writeBrtFmt(Biff12Writer w, int formatId, String formatString) throws IOException {
        byte[] strBytes = formatString.getBytes(StandardCharsets.UTF_16LE);
        int strLen = formatString.length();
        int recordSize = 2 + 4 + strLen * 2;
        
        w.writeRecordHeader(44, recordSize);
        w.writeShortLE(formatId);
        w.writeIntLE(strLen);
        w.writeBytes(strBytes);
    }
    
    private void writeFonts(Biff12Writer w) throws IOException {
        w.writeRecordHeader(611, 4);
        w.writeIntLE(1);
        
        writeBrtFont(w);
        
        w.writeEmptyRecord(612);
    }
    
    private void writeBrtFont(Biff12Writer w) throws IOException {
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
        w.writeRecordHeader(43, data.length);
        w.writeBytes(data);
    }
    
    private void writeFills(Biff12Writer w) throws IOException {
        w.writeRecordHeader(603, 4);
        w.writeIntLE(2);
        
        writeBrtFill(w, new byte[4]);
        writeBrtFill(w, new byte[]{0x02, 0x00, (byte)0x80, 0x00});
        
        w.writeEmptyRecord(604);
    }
    
    private void writeBrtFill(Biff12Writer w, byte[] data) throws IOException {
        w.writeRecordHeader(45, data.length);
        w.writeBytes(data);
    }
    
    private void writeBorders(Biff12Writer w) throws IOException {
        w.writeRecordHeader(613, 4);
        w.writeIntLE(1);
        
        byte[] data = new byte[24];
        w.writeRecordHeader(46, data.length);
        w.writeBytes(data);
        
        w.writeEmptyRecord(614);
    }
    
    private void writeCellStyleXFs(Biff12Writer w) throws IOException {
        writeBrtXF(w, 0xFFFF, 0, 0, 0, 0);
    }
    
    private void writeCellXFs(Biff12Writer w) throws IOException {
        List<CellStyleFormat> styles = styleRegistry.getStyles();
        for (int i = 0; i < styles.size(); i++) {
            CellStyleFormat style = styles.get(i);
            writeBrtXF(w, style.getNumFmtId(), style.getFontId(), style.getFillId(), style.getBorderId(), 0);
        }
    }
    
    private void writeBrtXF(Biff12Writer w, int numFmtId, int fontId, int fillId, int borderId, int xfId) throws IOException {
        byte[] data = new byte[16];
        
        data[0] = (byte)(numFmtId & 0xFF);
        data[1] = (byte)((numFmtId >> 8) & 0xFF);
        
        data[2] = (byte)(fontId & 0xFF);
        data[3] = (byte)((fontId >> 8) & 0xFF);
        
        data[4] = (byte)(fillId & 0xFF);
        data[5] = (byte)((fillId >> 8) & 0xFF);
        
        data[6] = (byte)(borderId & 0xFF);
        data[7] = (byte)((borderId >> 8) & 0xFF);
        
        data[8] = (byte)(xfId & 0xFF);
        data[9] = (byte)((xfId >> 8) & 0xFF);
        data[10] = (byte)((xfId >> 16) & 0xFF);
        data[11] = (byte)((xfId >> 24) & 0xFF);
        
        data[12] = 0x08;
        data[13] = 0x10;
        data[14] = 0x00;
        data[15] = 0x00;
        
        w.writeRecordHeader(47, data.length);
        w.writeBytes(data);
    }
    
    CellStyleRegistry getStyleRegistry() {
        return styleRegistry;
    }
}