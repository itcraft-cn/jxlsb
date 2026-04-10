package cn.itcraft.jxlsb.format.record.reader;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.Biff12RecordType;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BrtCellBool记录解析类
 * 
 * <p>根据MS-XLSB规范，BrtCellBool存储布尔值。
 * 
 * <p>记录结构：
 * <ul>
 *   <li>rw (4 bytes): 行索引</li>
 *   <li>col (4 bytes): 列索引</li>
 *   <li>ixfe (4 bytes): 样式索引</li>
 *   <li>value (1 byte): boolean值</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BrtCellBool extends BiffRecord {
    
    private static final int ROW_OFFSET = 0;
    private static final int COL_OFFSET = 4;
    private static final int STYLE_OFFSET = 8;
    private static final int VALUE_OFFSET = 12;
    
    public BrtCellBool(MemoryBlock dataBlock, int recordSize) {
        super(Biff12RecordType.BrtCellBool, recordSize, dataBlock);
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
    
    public boolean getValue() {
        return dataBlock.getByte(VALUE_OFFSET) != 0;
    }
    
    @Override
    public void writeTo(cn.itcraft.jxlsb.format.RecordWriter writer) {
        throw new UnsupportedOperationException("Reader record does not support write");
    }
}