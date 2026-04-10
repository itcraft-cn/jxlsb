package cn.itcraft.jxlsb.format.record.reader;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.Biff12RecordType;
import cn.itcraft.jxlsb.format.SharedStringsTable;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BrtCellSt记录解析类
 * 
 * <p>根据MS-XLSB规范，BrtCellSt引用SharedStringsTable中的字符串。
 * 
 * <p>记录结构：
 * <ul>
 *   <li>rw (4 bytes): 行索引</li>
 *   <li>col (4 bytes): 列索引</li>
 *   <li>ixfe (4 bytes): 样式索引</li>
 *   <li>isst (4 bytes): SST索引</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BrtCellSt extends BiffRecord {
    
    private static final int ROW_OFFSET = 0;
    private static final int COL_OFFSET = 4;
    private static final int STYLE_OFFSET = 8;
    private static final int SST_INDEX_OFFSET = 12;
    
    private final SharedStringsTable sst;
    
    public BrtCellSt(MemoryBlock dataBlock, int recordSize, SharedStringsTable sst) {
        super(Biff12RecordType.BrtCellSt, recordSize, dataBlock);
        this.sst = sst;
    }
    
    public int getRow() {
        return dataBlock.getInt(ROW_OFFSET);
    }
    
    public int getCol() {
        return dataBlock.getInt(COL_OFFSET);
    }
    
    public int getStyleId() {
        return dataBlock.getInt(STYLE_OFFSET);
    }
    
    public int getSstIndex() {
        return dataBlock.getInt(SST_INDEX_OFFSET);
    }
    
    public String getValue() {
        return sst.getString(getSstIndex());
    }
    
    @Override
    public void writeTo(cn.itcraft.jxlsb.format.RecordWriter writer) {
        throw new UnsupportedOperationException("Reader record does not support write");
    }
}