package cn.itcraft.jxlsb.data;

import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import java.util.Iterator;

/**
 * 堆外Sheet数据结构
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class OffHeapSheet implements AutoCloseable, Iterable<OffHeapRow> {
    
    private final String sheetName;
    private final int sheetIndex;
    private final int rowCount;
    private final int columnCount;
    private final OffHeapAllocator allocator;
    
    private OffHeapRow currentRow;
    private int currentRowIndex = -1;
    
    public OffHeapSheet(String sheetName, int sheetIndex, 
                       int rowCount, int columnCount) {
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    public OffHeapRow nextRow() {
        if (currentRowIndex >= rowCount - 1) {
            return null;
        }
        
        if (currentRow != null) {
            currentRow.close();
        }
        
        currentRowIndex++;
        currentRow = new OffHeapRow(currentRowIndex, columnCount, allocator);
        return currentRow;
    }
    
    public OffHeapRow currentRow() {
        return currentRow;
    }
    
    public OffHeapRow createRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("Row index out of bounds");
        }
        return new OffHeapRow(rowIndex, columnCount, allocator);
    }
    
    public String getSheetName() {
        return sheetName;
    }
    
    public int getSheetIndex() {
        return sheetIndex;
    }
    
    public int getRowCount() {
        return rowCount;
    }
    
    public int getColumnCount() {
        return columnCount;
    }
    
    @Override
    public Iterator<OffHeapRow> iterator() {
        return new SheetRowIterator(this);
    }
    
    @Override
    public void close() {
        if (currentRow != null) {
            currentRow.close();
        }
    }
    
    private static final class SheetRowIterator implements Iterator<OffHeapRow> {
        private final OffHeapSheet sheet;
        
        SheetRowIterator(OffHeapSheet sheet) {
            this.sheet = sheet;
        }
        
        @Override
        public boolean hasNext() {
            return sheet.currentRowIndex < sheet.rowCount - 1;
        }
        
        @Override
        public OffHeapRow next() {
            return sheet.nextRow();
        }
    }
}