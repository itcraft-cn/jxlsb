package cn.itcraft.jxlsb.format.record;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.data.CellType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Extended Record Types Test")
class ExtendedRecordTypesTest {
    
    @Test
    @DisplayName("Should create and read IndexRecord")
    void shouldCreateAndReadIndexRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        IndexRecord record = IndexRecord.create(0, 100, 0, 50, block);
        
        assertEquals(IndexRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(0, record.getFirstRow());
        assertEquals(100, record.getLastRow());
        assertEquals(0, record.getFirstColumn());
        assertEquals(50, record.getLastColumn());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read FormatRecord")
    void shouldCreateAndReadFormatRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(256);
        
        FormatRecord record = FormatRecord.create(
            FormatRecord.FORMAT_DATE, 
            "yyyy-mm-dd", 
            block);
        
        assertEquals(FormatRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(FormatRecord.FORMAT_DATE, record.getFormatIndex());
        assertTrue(record.getFormatString().contains("yyyy"));
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read XFRecord")
    void shouldCreateAndReadXFRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        XFRecord record = XFRecord.create(
            0, FormatRecord.FORMAT_NUMBER,
            XFRecord.ALIGN_RIGHT, XFRecord.VERTICAL_CENTER,
            0, 0, block);
        
        assertEquals(XFRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(0, record.getFontIndex());
        assertEquals(FormatRecord.FORMAT_NUMBER, record.getFormatIndex());
        assertEquals(XFRecord.ALIGN_RIGHT, record.getHorizontalAlignment());
        assertEquals(XFRecord.VERTICAL_CENTER, record.getVerticalAlignment());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read FormulaRecord")
    void shouldCreateAndReadFormulaRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(2048);
        
        FormulaRecord record = FormulaRecord.create(
            10, 5,
            CellType.NUMBER, 42.0,
            "SUM(A1:A10)", block);
        
        assertEquals(FormulaRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(10, record.getRowIndex());
        assertEquals(5, record.getColIndex());
        assertEquals(CellType.NUMBER, record.getResultType());
        assertEquals(42.0, record.getNumberResult(), 0.001);
        assertEquals("SUM(A1:A10)", record.getFormula());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read MergeCellRecord")
    void shouldCreateAndReadMergeCellRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        MergeCellRecord record = MergeCellRecord.create(0, 2, 0, 3, block);
        
        assertEquals(MergeCellRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(0, record.getFirstRow());
        assertEquals(2, record.getLastRow());
        assertEquals(0, record.getFirstCol());
        assertEquals(3, record.getLastCol());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read ConditionalFormatRecord")
    void shouldCreateAndReadConditionalFormatRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(256);
        
        ConditionalFormatRecord record = ConditionalFormatRecord.create(
            ConditionalFormatRecord.TYPE_CELL_IS,
            ConditionalFormatRecord.OPERATOR_GREATER_THAN,
            "100", block);
        
        assertEquals(ConditionalFormatRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(ConditionalFormatRecord.TYPE_CELL_IS, record.getType());
        assertEquals(ConditionalFormatRecord.OPERATOR_GREATER_THAN, record.getOperator());
        assertEquals("100", record.getFormula1());
        
        record.close();
    }
    
    @Test
    @DisplayName("Should create and read DataValidationRecord")
    void shouldCreateAndReadDataValidationRecord() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(256);
        
        DataValidationRecord record = DataValidationRecord.create(
            DataValidationRecord.TYPE_WHOLE,
            DataValidationRecord.ERROR_STYLE_STOP,
            false,
            "BETWEEN 1 AND 100", block);
        
        assertEquals(DataValidationRecord.RECORD_TYPE, record.getRecordType());
        assertEquals(DataValidationRecord.TYPE_WHOLE, record.getType());
        assertEquals(DataValidationRecord.ERROR_STYLE_STOP, record.getErrorStyle());
        assertFalse(record.isAllowBlank());
        assertTrue(record.getFormula1().contains("BETWEEN"));
        
        record.close();
    }
}