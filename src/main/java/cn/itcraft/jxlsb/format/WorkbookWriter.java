package cn.itcraft.jxlsb.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Workbook.bin写入器
 * 使用参考文件的准确数据
 */
public final class WorkbookWriter {
    
    private final List<SheetInfo> sheets = new ArrayList<>();
    
    public void addSheet(String name) {
        sheets.add(new SheetInfo(name, sheets.size() + 1));
    }
    
    public int getSheetCount() {
        return sheets.size();
    }
    
    public byte[] toBiff12Bytes() throws IOException {
        // workbook.bin通常很小（几百字节），设置4KB缓冲区足够
        Biff12Writer w = new Biff12Writer(4 * 1024);
        
        w.writeEmptyRecord(Biff12RecordType.BrtBeginBook);
        
        writeBrtFileVersion(w);
        
        writeBrtWbProp(w);
        
        w.writeEmptyRecord(Biff12RecordType.BrtBeginBookViews);
        
        writeBrtBookView(w);
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndBookViews);
        
        w.writeEmptyRecord(Biff12RecordType.BrtBeginBundleShs);
        
        for (SheetInfo sheet : sheets) {
            writeBrtBundleSh(w, sheet);
        }
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndBundleShs);
        
        w.writeEmptyRecord(Biff12RecordType.BrtEndBook);
        
        return w.toByteArray();
    }
    
    private void writeBrtFileVersion(Biff12Writer w) throws IOException {
        byte[] data = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00,
            0x00, 0x00, 0x78, 0x00, 0x6c, 0x00, 0x01, 0x00,
            0x00, 0x00, 0x33, 0x00, 0x01, 0x00, 0x00, 0x00,
            0x35, 0x00, 0x04, 0x00, 0x00, 0x00, 0x39, 0x00,
            0x33, 0x00, 0x30, 0x00, 0x32, 0x00, 0x00
        };
        w.writeRecordHeader(128, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtWbProp(Biff12Writer w) throws IOException {
        byte[] data = {
            0x20, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        };
        w.writeRecordHeader(153, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtBookView(Biff12Writer w) throws IOException {
        byte[] data = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            (byte)0x80, 0x70, 0x00, 0x00, (byte)0xcf, 0x30, 0x00, 0x00,
            0x58, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x78
        };
        w.writeRecordHeader(158, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtBundleSh(Biff12Writer w, SheetInfo sheet) throws IOException {
        String relId = "rId" + sheet.sheetId;
        int relIdLen = relId.length();
        int nameLen = sheet.name.length();
        
        int recordSize = 4 + 4 + (4 + relIdLen * 2) + (4 + nameLen * 2);
        
        w.writeRecordHeader(Biff12RecordType.BrtBundleSh, recordSize);
        
        w.writeIntLE(0);
        
        w.writeIntLE(1);
        
        w.writeXLWideString(relId);
        
        w.writeXLWideString(sheet.name);
    }
    
    private static final class SheetInfo {
        final String name;
        final int sheetId;
        
        SheetInfo(String name, int sheetId) {
            this.name = name;
            this.sheetId = sheetId;
        }
    }
}