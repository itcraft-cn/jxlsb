package cn.itcraft.jxlsb.format.record.reader;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.Biff12RecordType;
import cn.itcraft.jxlsb.memory.MemoryBlock;

/**
 * BrtCellRk记录解析类
 * 
 * <p>根据MS-XLSB规范[MS-XLSB] 2.5.6节，BrtCellRk存储RK编码的数字值。
 * RK编码是一种压缩的IEEE 754 double格式，占用4字节。
 * 
 * <p>记录结构：
 * <ul>
 *   <li>rw (4 bytes): 行索引</li>
 *   <li>col (4 bytes): 列索引</li>
 *   <li>ixfe (4 bytes): 样式索引</li>
 *   <li>RK (4 bytes): RK编码的数值</li>
 * </ul>
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class BrtCellRk extends BiffRecord {
    
    private static final int ROW_OFFSET = 0;
    private static final int COL_OFFSET = 4;
    private static final int STYLE_OFFSET = 8;
    private static final int RK_VALUE_OFFSET = 12;
    
    public BrtCellRk(MemoryBlock dataBlock, int recordSize) {
        super(Biff12RecordType.BrtCellRk, recordSize, dataBlock);
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
    
    public double getValue() {
        int rkValue = dataBlock.getInt(RK_VALUE_OFFSET);
        return decodeRk(rkValue);
    }
    
    /**
     * RK编码解码
     * 
     * <p>根据MS-XLSB规范2.5.6节：
     * <ul>
     *   <li>Bit 0: isInt标志（0=IEEE float, 1=integer）</li>
     *   <li>Bit 1: div100标志（0=原值, 1=除以100）</li>
     *   <li>Bits 2-31: IEEE 754 double的高30位或整数值</li>
     * </ul>
     */
    private double decodeRk(int rkValue) {
        boolean isInt = (rkValue & 0x01) != 0;
        boolean div100 = (rkValue & 0x02) != 0;
        
        int valueBits = rkValue & 0xFFFFFFFC;
        
        double value;
        if (isInt) {
            value = valueBits >> 2;
        } else {
            long doubleBits = ((long)valueBits << 32);
            value = Double.longBitsToDouble(doubleBits);
        }
        
        if (div100) {
            value /= 100.0;
        }
        
        return value;
    }
    
    @Override
    public void writeTo(cn.itcraft.jxlsb.format.RecordWriter writer) {
        throw new UnsupportedOperationException("Reader record does not support write");
    }
}