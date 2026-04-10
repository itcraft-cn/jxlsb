package cn.itcraft.jxlsb.data;

import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 堆外Workbook数据结构
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class OffHeapWorkbook implements AutoCloseable {
    
    private final List<OffHeapSheet> sheets;
    private final OffHeapAllocator allocator;
    
    public OffHeapWorkbook() {
        this.sheets = new ArrayList<>();
        this.allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    public OffHeapSheet getSheet(int sheetIndex) {
        if (sheetIndex < 0 || sheetIndex >= sheets.size()) {
            throw new IndexOutOfBoundsException("Sheet index out of bounds");
        }
        return sheets.get(sheetIndex);
    }
    
    public OffHeapSheet createSheet(String sheetName, int rowCount, int columnCount) {
        OffHeapSheet sheet = new OffHeapSheet(sheetName, sheets.size(), rowCount, columnCount);
        sheets.add(sheet);
        return sheet;
    }
    
    public int getSheetCount() {
        return sheets.size();
    }
    
    @Override
    public void close() {
        for (OffHeapSheet sheet : sheets) {
            sheet.close();
        }
        sheets.clear();
    }
}