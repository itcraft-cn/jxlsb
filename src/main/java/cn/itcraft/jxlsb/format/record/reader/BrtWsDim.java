package cn.itcraft.jxlsb.format.record.reader;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.Biff12RecordType;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BrtWsDim记录解析类
 * 
 * <p>根据MS-XLSB规范，BrtWsDim存储Sheet维度信息。
 * 
 * <p>记录结构：
 * <ul>
 *   <li>rwFirst (4 bytes): 第一行</li>
 *   <li>rwLast (4 bytes): 最后一行</li>
 *   <li>colFirst (4 bytes): 第一列</li>
 *   <li>colLast (4 bytes): 最后一列</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BrtWsDim extends BiffRecord {
    
    private static final int FIRST_ROW_OFFSET = 0;
    private static final int LAST_ROW_OFFSET = 4;
    private static final int FIRST_COL_OFFSET = 8;
    private static final int LAST_COL_OFFSET = 12;
    
    public BrtWsDim(MemoryBlock dataBlock, int recordSize) {
        super(Biff12RecordType.BrtWsDim, recordSize, dataBlock);
    }
    
    public int getFirstRow() {
        return dataBlock.getInt(FIRST_ROW_OFFSET);
    }
    
    public int getLastRow() {
        return dataBlock.getInt(LAST_ROW_OFFSET);
    }
    
    public int getFirstColumn() {
        return dataBlock.getInt(FIRST_COL_OFFSET);
    }
    
    public int getLastColumn() {
        return dataBlock.getInt(LAST_COL_OFFSET);
    }
    
    public int getRowCount() {
        return getLastRow() - getFirstRow() + 1;
    }
    
    public int getColumnCount() {
        return getLastColumn() - getFirstColumn() + 1;
    }
    
    @Override
    public void writeTo(cn.itcraft.jxlsb.format.RecordWriter writer) {
        throw new UnsupportedOperationException("Reader record does not support write");
    }
}