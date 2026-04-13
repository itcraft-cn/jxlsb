package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReadRowsTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testReadRowsNumbers() throws IOException {
        Path file = tempDir.resolve("numbers.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> CellData.number(row * 10 + col), 100, 3);
        }
        
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            List<CellData[]> batch = reader.readRows(0, 0, 50);
            assertEquals(50, batch.size());
            assertEquals(3, batch.get(0).length);
            assertEquals(0.0, batch.get(0)[0].getValue());
            assertEquals(1.0, batch.get(0)[1].getValue());
        }
    }
    
    @Test
    void testReadRowsWithText() throws IOException {
        Path file = tempDir.resolve("text.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> {
                if (col == 0) return CellData.number(row);
                return CellData.text("T-" + row);
            }, 50, 3);
        }
        
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            List<CellData[]> batch = reader.readRows(0, 0, 50);
            assertEquals(50, batch.size());
            assertEquals("T-0", batch.get(0)[1].getValue());
        }
    }
    
    @Test
    void testReadRowsPagination() throws IOException {
        Path file = tempDir.resolve("pagination.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> CellData.number(row), 100, 1);
        }
        
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            List<CellData[]> batch1 = reader.readRows(0, 0, 30);
            assertEquals(30, batch1.size());
            
            List<CellData[]> batch2 = reader.readRows(0, 30, 30);
            assertEquals(30, batch2.size());
            
            List<CellData[]> batch3 = reader.readRows(0, 60, 40);
            assertEquals(40, batch3.size());
            
            List<CellData[]> batch4 = reader.readRows(0, 100, 10);
            assertEquals(0, batch4.size());
        }
    }
}