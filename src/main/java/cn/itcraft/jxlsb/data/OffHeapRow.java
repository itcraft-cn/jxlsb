package cn.itcraft.jxlsb.data;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;

/**
 * 堆外行数据结构
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class OffHeapRow implements AutoCloseable {
    
    private static final long CELL_BLOCK_SIZE = 1024L;
    
    private final MemoryBlock[] cellBlocks;
    private final int rowIndex;
    private final int columnCount;
    private final OffHeapAllocator allocator;
    
    public OffHeapRow(int rowIndex, int columnCount, OffHeapAllocator allocator) {
        this.rowIndex = rowIndex;
        this.columnCount = columnCount;
        this.allocator = allocator;
        this.cellBlocks = new MemoryBlock[columnCount];
        
        for (int i = 0; i < columnCount; i++) {
            cellBlocks[i] = allocator.allocateFromPool(CELL_BLOCK_SIZE);
        }
    }
    
    public OffHeapCell getCell(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new IndexOutOfBoundsException("Column index out of bounds");
        }
        return new OffHeapCell(cellBlocks[columnIndex], rowIndex, columnIndex);
    }
    
    public int getRowIndex() {
        return rowIndex;
    }
    
    public int getColumnCount() {
        return columnCount;
    }
    
    @Override
    public void close() {
        for (MemoryBlock block : cellBlocks) {
            if (block != null) {
                block.close();
            }
        }
    }
}