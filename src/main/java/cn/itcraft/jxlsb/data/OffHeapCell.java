package cn.itcraft.jxlsb.data;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import java.nio.charset.StandardCharsets;

/**
 * 堆外单元格数据结构
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class OffHeapCell implements AutoCloseable {
    
    private static final int TYPE_OFFSET = 0;
    private static final int VALUE_OFFSET = 4;
    private static final int MAX_TEXT_SIZE = 1020;
    
    private final MemoryBlock memoryBlock;
    private final int rowIndex;
    private final int colIndex;
    
    public OffHeapCell(MemoryBlock memoryBlock, int rowIndex, int colIndex) {
        this.memoryBlock = memoryBlock;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
    }
    
    public void setText(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_SIZE) {
            bytes = new byte[MAX_TEXT_SIZE];
            System.arraycopy(text.getBytes(StandardCharsets.UTF_8), 0, bytes, 0, MAX_TEXT_SIZE);
        }
        memoryBlock.putInt(TYPE_OFFSET, CellType.TEXT.getCode());
        memoryBlock.putInt(VALUE_OFFSET, bytes.length);
        memoryBlock.putBytes(VALUE_OFFSET + 4, bytes, 0, bytes.length);
    }
    
    public void setNumber(double value) {
        memoryBlock.putInt(TYPE_OFFSET, CellType.NUMBER.getCode());
        memoryBlock.putDouble(VALUE_OFFSET, value);
    }
    
    public void setDate(long timestamp) {
        memoryBlock.putInt(TYPE_OFFSET, CellType.DATE.getCode());
        memoryBlock.putLong(VALUE_OFFSET, timestamp);
    }
    
    public void setBoolean(boolean value) {
        memoryBlock.putInt(TYPE_OFFSET, CellType.BOOLEAN.getCode());
        memoryBlock.putByte(VALUE_OFFSET, (byte) (value ? 1 : 0));
    }
    
    public CellType getType() {
        int code = memoryBlock.getInt(TYPE_OFFSET);
        return CellType.fromCode(code);
    }
    
    public String getText() {
        if (getType() != CellType.TEXT) {
            throw new IllegalStateException("Cell is not text type");
        }
        int length = memoryBlock.getInt(VALUE_OFFSET);
        byte[] bytes = new byte[length];
        memoryBlock.getBytes(VALUE_OFFSET + 4, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public double getNumber() {
        if (getType() != CellType.NUMBER) {
            throw new IllegalStateException("Cell is not number type");
        }
        return memoryBlock.getDouble(VALUE_OFFSET);
    }
    
    public long getDate() {
        if (getType() != CellType.DATE) {
            throw new IllegalStateException("Cell is not date type");
        }
        return memoryBlock.getLong(VALUE_OFFSET);
    }
    
    public boolean getBoolean() {
        if (getType() != CellType.BOOLEAN) {
            throw new IllegalStateException("Cell is not boolean type");
        }
        return memoryBlock.getByte(VALUE_OFFSET) != 0;
    }
    
    public int getRowIndex() {
        return rowIndex;
    }
    
    public int getColIndex() {
        return colIndex;
    }
    
    @Override
    public void close() {
        memoryBlock.close();
    }
}