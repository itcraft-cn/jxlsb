package cn.itcraft.jxlsb.format;

import java.io.IOException;

/**
 * 最小化的styles.bin写入器
 * 根据MS-XLSB规范实现
 */
public final class StylesWriter {
    
    // 记录类型编号
    private static final int BrtFont = 43;
    private static final int BrtFill = 45;
    private static final int BrtBorder = 46;
    private static final int BrtXF = 47;
    private static final int BrtBeginStyleSheet = 278;
    private static final int BrtEndStyleSheet = 279;
    private static final int BrtBeginCellStyleXFs = 626;
    private static final int BrtEndCellStyleXFs = 627;
    private static final int BrtBeginCellXFs = 617;
    private static final int BrtEndCellXFs = 618;
    
    public byte[] toBiff12Bytes() throws IOException {
        Biff12Writer w = new Biff12Writer();
        
        // BrtBeginStyleSheet
        w.writeEmptyRecord(BrtBeginStyleSheet);
        
        // BrtBeginCellStyleXFs - cell style XFs
        w.writeRecordHeader(BrtBeginCellStyleXFs, 4);
        w.writeIntLE(1); // cxfs = 1
        
        // BrtXF (style XF)
        writeStyleXF(w);
        
        // BrtEndCellStyleXFs
        w.writeEmptyRecord(BrtEndCellStyleXFs);
        
        // BrtBeginCellXFs - cell XFs
        w.writeRecordHeader(BrtBeginCellXFs, 4);
        w.writeIntLE(1); // cxfs = 1
        
        // BrtXF (cell XF)
        writeCellXF(w);
        
        // BrtEndCellXFs
        w.writeEmptyRecord(BrtEndCellXFs);
        
        // BrtEndStyleSheet
        w.writeEmptyRecord(BrtEndStyleSheet);
        
        return w.toByteArray();
    }
    
    private void writeStyleXF(Biff12Writer w) throws IOException {
        // BrtXF结构 - style XF (fStyle=1)
        // 参考 2.4.876
        w.writeRecordHeader(BrtXF, 20);
        
        // ixfeParent (2 bytes)
        w.writeBytes(new byte[]{(byte)0xFF, (byte)0xFF}); // 0xFFFF for style XF
        
        // ifmt (2 bytes) - format index
        w.writeBytes(new byte[]{0, 0});
        
        // ixfont (2 bytes) - font index
        w.writeBytes(new byte[]{0, 0});
        
        // trot (1 byte) - text rotation
        w.writeBytes(new byte[]{0});
        
        // cIndent (1 byte)
        w.writeBytes(new byte[]{0});
        
        // flags (4 bytes) - fStyle=1 (bit 2)
        w.writeIntLE(0x04);
        
        // remaining (8 bytes)
        w.writeBytes(new byte[8]);
    }
    
    private void writeCellXF(Biff12Writer w) throws IOException {
        // BrtXF结构 - cell XF (fStyle=0)
        w.writeRecordHeader(BrtXF, 20);
        
        // ixfeParent (2 bytes) - parent style index
        w.writeBytes(new byte[]{0, 0});
        
        // ifmt (2 bytes)
        w.writeBytes(new byte[]{0, 0});
        
        // ixfont (2 bytes)
        w.writeBytes(new byte[]{0, 0});
        
        // trot (1 byte)
        w.writeBytes(new byte[]{0});
        
        // cIndent (1 byte)
        w.writeBytes(new byte[]{0});
        
        // flags (4 bytes) - fStyle=0
        w.writeIntLE(0);
        
        // remaining (8 bytes)
        w.writeBytes(new byte[8]);
    }
}