package cn.itcraft.jxlsb.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Workbook.bin写入器
 * 严格按照MS-XLSB规范实现
 */
public final class WorkbookWriter {
    
    private final List<SheetInfo> sheets = new ArrayList<>();
    
    public void addSheet(String name) {
        sheets.add(new SheetInfo(name, sheets.size() + 1));
    }
    
    public int getSheetCount() {
        return sheets.size();
    }
    
    /**
     * 生成workbook.bin内容
     * 记录序列：BrtBeginBook -> BrtFileVersion -> BrtWbProp -> BrtBeginBookViews -> ... -> BrtEndBook
     */
    public byte[] toBiff12Bytes() throws IOException {
        Biff12Writer w = new Biff12Writer();
        
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
            0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00,
            'x', 0, 'l', 0
        };
        w.writeRecordHeader(128, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtWbProp(Biff12Writer w) throws IOException {
        byte[] data = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00,
            0x00, 0x33, 0x00, 0x01, 0x00, 0x00, 0x00, 0x35,
            0x00, 0x04, 0x00, 0x00, 0x00, 0x39, 0x00, 0x33,
            0x00, 0x30, 0x00, 0x32, 0x00
        };
        w.writeRecordHeader(153, data.length);
        w.writeBytes(data);
    }
    
    private void writeBrtBookView(Biff12Writer w) throws IOException {
        byte[] data = {
            0x20, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        };
        w.writeRecordHeader(156, data.length);
        w.writeBytes(data);
    }
    
    /**
     * 写入BrtBundleSh记录
     * 结构：hsState(1) + iTabID(4) + strRelID(变量) + strName(变量)
     */
    private void writeBrtBundleSh(Biff12Writer w, SheetInfo sheet) throws IOException {
        // 先计算大小
        int nameLen = sheet.name.length();
        int strLen = 4 + nameLen * 2;  // 字符数(4) + UTF-16LE字符
        
        // strRelID 固定为 "rId{N}"
        String relId = "rId" + sheet.sheetId;
        int relIdLen = 4 + relId.length() * 2;
        
        int recordSize = 1 + 4 + relIdLen + strLen;  // hsState + iTabID + strRelID + strName
        
        w.writeRecordHeader(Biff12RecordType.BrtBundleSh, recordSize);
        
        // hsState (1 byte) - 0 = visible
        w.writeBytes(new byte[]{0});
        
        // iTabID (4 bytes)
        w.writeIntLE(sheet.sheetId);
        
        // strRelID (XLWideString)
        w.writeXLWideString(relId);
        
        // strName (XLWideString)
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