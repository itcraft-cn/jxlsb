package cn.itcraft.jxlsb.format.record;
import java.io.IOException;

import cn.itcraft.jxlsb.format.BiffRecord;
import cn.itcraft.jxlsb.format.RecordWriter;
import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.data.CellType;
import java.nio.charset.StandardCharsets;

/**
 * FormulaRecord（公式记录）
 * 
 * <p>BIFF12记录类型0x0108，存储单元格公式及其计算结果。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class FormulaRecord extends BiffRecord {
    
    public static final int RECORD_TYPE = 0x0108;
    
    private static final int ROW_INDEX_OFFSET = 0;
    private static final int COL_INDEX_OFFSET = 4;
    private static final int RESULT_TYPE_OFFSET = 8;
    private static final int RESULT_VALUE_OFFSET = 12;
    private static final int FORMULA_LENGTH_OFFSET = 20;
    private static final int FORMULA_DATA_OFFSET = 24;
    
    public FormulaRecord(int recordSize, MemoryBlock dataBlock) {
        super(RECORD_TYPE, recordSize, dataBlock);
    }
    
    public static FormulaRecord create(int rowIndex, int colIndex,
                                       CellType resultType, Object resultValue,
                                       String formula, MemoryBlock dataBlock) {
        dataBlock.putInt(ROW_INDEX_OFFSET, rowIndex);
        dataBlock.putInt(COL_INDEX_OFFSET, colIndex);
        dataBlock.putInt(RESULT_TYPE_OFFSET, resultType.getCode());
        
        switch (resultType) {
            case NUMBER:
                dataBlock.putDouble(RESULT_VALUE_OFFSET, (Double) resultValue);
                break;
            case BOOLEAN:
                dataBlock.putByte(RESULT_VALUE_OFFSET, (Boolean) resultValue ? (byte) 1 : (byte) 0);
                break;
            case TEXT:
                String text = (String) resultValue;
                byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
                dataBlock.putInt(RESULT_VALUE_OFFSET, textBytes.length);
                dataBlock.putBytes(RESULT_VALUE_OFFSET + 4, textBytes, 0, 
                    Math.min(textBytes.length, 100));
                break;
            default:
                break;
        }
        
        byte[] formulaBytes = formula.getBytes(StandardCharsets.UTF_8);
        int formulaLength = Math.min(formulaBytes.length, 65535);
        
        dataBlock.putInt(FORMULA_LENGTH_OFFSET, formulaLength);
        dataBlock.putBytes(FORMULA_DATA_OFFSET, formulaBytes, 0, formulaLength);
        
        return new FormulaRecord(24 + formulaLength, dataBlock);
    }
    
    public int getRowIndex() {
        return dataBlock.getInt(ROW_INDEX_OFFSET);
    }
    
    public int getColIndex() {
        return dataBlock.getInt(COL_INDEX_OFFSET);
    }
    
    public CellType getResultType() {
        int code = dataBlock.getInt(RESULT_TYPE_OFFSET);
        return CellType.fromCode(code);
    }
    
    public double getNumberResult() {
        return dataBlock.getDouble(RESULT_VALUE_OFFSET);
    }
    
    public boolean getBooleanResult() {
        return dataBlock.getByte(RESULT_VALUE_OFFSET) != 0;
    }
    
    public int getFormulaLength() {
        return dataBlock.getInt(FORMULA_LENGTH_OFFSET);
    }
    
    public String getFormula() {
        int length = getFormulaLength();
        byte[] bytes = new byte[length];
        dataBlock.getBytes(FORMULA_DATA_OFFSET, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public void writeTo(RecordWriter writer) throws IOException {
        writer.writeRecordHeader(RECORD_TYPE, recordSize);
        
        byte[] data = new byte[recordSize];
        dataBlock.getBytes(0, data, 0, recordSize);
        writer.writeBytes(data);
    }
}