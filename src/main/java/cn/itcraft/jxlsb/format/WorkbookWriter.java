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
        
        // BrtBeginBook (empty record)
        w.writeEmptyRecord(Biff12RecordType.BrtBeginBook);
        
        // BrtFileVersion - 可选，省略以简化
        
        // BrtWbProp - 可选，省略以简化
        
        // BrtBeginBookViews - 可选，省略以简化
        
        // BrtBeginBundleShs (开始Sheet集合)
        w.writeEmptyRecord(Biff12RecordType.BrtBeginBundleShs);
        
        // BrtBundleSh records (每个Sheet一个)
        for (SheetInfo sheet : sheets) {
            writeBrtBundleSh(w, sheet);
        }
        
        // BrtEndBundleShs
        w.writeEmptyRecord(Biff12RecordType.BrtEndBundleShs);
        
        // BrtEndBook
        w.writeEmptyRecord(Biff12RecordType.BrtEndBook);
        
        return w.toByteArray();
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