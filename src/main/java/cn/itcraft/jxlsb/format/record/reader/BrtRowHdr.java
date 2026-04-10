package cn.itcraft.jxlsb.format.record.reader;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.Biff12RecordType;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BrtRowHdr记录解析类
 * 
 * <p>根据MS-XLSB规范，BrtRowHdr存储行头信息。
 * 
 * <p>记录结构：
 * <ul>
 *   <li>rw (4 bytes): 行索引</li>
 *   <li>colFirst (4 bytes): 第一列索引</li>
 *   <li>colLast (4 bytes): 最后一列索引</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BrtRowHdr extends BiffRecord {
    
    private static final int ROW_OFFSET = 0;
    private static final int FIRST_COL_OFFSET = 4;
    private static final int LAST_COL_OFFSET = 8;
    
    public BrtRowHdr(MemoryBlock dataBlock, int recordSize) {
        super(Biff12RecordType.BrtRowHdr, recordSize, dataBlock);
    }
    
    public int getRowIndex() {
        return dataBlock.getInt(ROW_OFFSET);
    }
    
    public int getFirstColumn() {
        return dataBlock.getInt(FIRST_COL_OFFSET);
    }
    
    public int getLastColumn() {
        return dataBlock.getInt(LAST_COL_OFFSET);
    }
    
    public int getColumnCount() {
        return getLastColumn() - getFirstColumn() + 1;
    }
    
    @Override
    public void writeTo(cn.itcraft.jxlsb.format.RecordWriter writer) {
        throw new UnsupportedOperationException("Reader record does not support write");
    }
}