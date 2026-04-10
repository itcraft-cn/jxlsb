package cn.itcraft.jxlsb.format.record.reader;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.Biff12RecordType;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import java.nio.charset.StandardCharsets;

/**
 * BrtCellIsst记录解析类
 * 
 * <p>根据MS-XLSB规范，BrtCellIsst存储内联字符串。
 * 
 * <p>记录结构：
 * <ul>
 *   <li>rw (4 bytes): 行索引</li>
 *   <li>col (4 bytes): 列索引</li>
 *   <li>ixfe (4 bytes): 样式索引</li>
 *   <li>XLWideString: UTF-16LE编码字符串</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BrtCellIsst extends BiffRecord {
    
    private static final int ROW_OFFSET = 0;
    private static final int COL_OFFSET = 4;
    private static final int STYLE_OFFSET = 8;
    private static final int STRING_OFFSET = 12;
    
    public BrtCellIsst(MemoryBlock dataBlock, int recordSize) {
        super(Biff12RecordType.BrtCellIsst, recordSize, dataBlock);
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
    
    public String getValue() {
        return readXLWideString(dataBlock, STRING_OFFSET);
    }
    
    private String readXLWideString(MemoryBlock block, int offset) {
        int length = block.getInt(offset);
        if (length == 0) {
            return "";
        }
        
        byte[] bytes = new byte[length * 2];
        block.getBytes(offset + 4, bytes, 0, bytes.length);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }
    
    @Override
    public void writeTo(cn.itcraft.jxlsb.format.RecordWriter writer) {
        throw new UnsupportedOperationException("Reader record does not support write");
    }
}