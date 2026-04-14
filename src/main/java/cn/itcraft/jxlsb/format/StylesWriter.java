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
        
        w.writeEmptyRecord(278);  // BrtBeginCellStyleXFs
        
        writeFormats(w);
        writeFonts(w);
        writeFills(w);
        writeBorders(w);
        
        writeCellStyleXFs2(w);
        
        writeStyles(w);
        
        w.writeEmptyRecord(279);  // BrtEndCellStyleXFs
        
        return w.toByteArray();
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
        byte[] data = {
            (byte)0xdc, 0x00, 0x00, 0x00,
            (byte)0x90, 0x01, 0x00, 0x00,
            0x00, 0x00,
            (byte)0x86, 0x00,
            0x07, 0x01,
            0x00, 0x00, 0x00, 0x00, 0x00,
            (byte)0xff,
            0x02, 0x02, 0x00, 0x00, 0x00,
            (byte)0x8b, (byte)0x5b, (byte)0x53, (byte)0x4f
        };
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
    
    private void writeCellStyleXFs2(Biff12Writer w) throws IOException {
        w.writeRecordHeader(626, 4);
        w.writeIntLE(1);
        
        byte[] data = new byte[16];
        data[0] = (byte)0xFF;
        data[1] = (byte)0xFF;
        data[12] = 0x08;
        data[13] = 0x10;
        data[14] = 0x00;
        data[15] = 0x00;
        w.writeRecordHeader(47, 16);
        w.writeBytes(data);
        
        w.writeEmptyRecord(627);
    }
    
    private void writeStyles(Biff12Writer w) throws IOException {
        List<CellStyleFormat> styles = styleRegistry.getStyles();
        int count = styles.size();
        
        w.writeRecordHeader(617, 4);
        w.writeIntLE(count);
        
        for (int i = 0; i < styles.size(); i++) {
            CellStyleFormat style = styles.get(i);
            writeStyleXF(w, style.getNumFmtId(), i == 0);
        }
        
        w.writeEmptyRecord(618);
    }
    
    private void writeStyleXF(Biff12Writer w, int formatId, boolean isFirst) throws IOException {
        byte[] data = new byte[16];
        
        data[0] = 0x00;
        data[1] = 0x00;
        
        data[2] = (byte)(formatId & 0xFF);
        data[3] = (byte)((formatId >> 8) & 0xFF);
        
        data[12] = 0x08;
        data[13] = 0x10;
        if (!isFirst) {
            data[14] = 0x01;
        }
        data[15] = 0x00;
        
        w.writeRecordHeader(47, 16);
        w.writeBytes(data);
    }
    
    CellStyleRegistry getStyleRegistry() {
        return styleRegistry;
    }
}