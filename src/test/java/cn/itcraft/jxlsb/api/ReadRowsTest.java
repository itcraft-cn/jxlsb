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
    void testReadRows() throws IOException {
        Path file = tempDir.resolve("test.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> {
                switch (col % 4) {
                    case 0: return CellData.number(row * 100 + col);
                    case 1: return CellData.text("Text-" + row);
                    case 2: return CellData.bool(row % 2 == 0);
                    default: return CellData.blank();
                }
            }, 1000, 5);
        }
        
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            List<CellData[]> batch1 = reader.readRows(0, 0, 100);
            assertEquals(100, batch1.size());
            
            List<CellData[]> batch2 = reader.readRows(0, 100, 100);
            assertEquals(100, batch2.size());
            
            List<CellData[]> batch3 = reader.readRows(0, 900, 100);
            assertEquals(100, batch3.size());
            
            List<CellData[]> batch4 = reader.readRows(0, 2000, 100);
            assertEquals(0, batch4.size());
            
            CellData[] row0 = batch1.get(0);
            assertEquals(0.0, row0[0].getValue());
            assertEquals("Text-0", row0[1].getValue());
        }
    }
    
    @Test
    void testReadRowsAsArray() throws IOException {
        Path file = tempDir.resolve("array.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> CellData.number(row + col), 500, 3);
        }
        
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            CellData[][] batch = reader.readRowsAsArray(0, 0, 50);
            assertEquals(50, batch.length);
            assertEquals(3, batch[0].length);
        }
    }
    
    @Test
    void testPaginationLoop() throws IOException {
        Path file = tempDir.resolve("pagination.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Data", (row, col) -> CellData.number(row), 1000, 1);
        }
        
        try (XlsbReader reader = XlsbReader.builder().path(file).build()) {
            int offset = 0;
            int batchSize = 100;
            int totalRead = 0;
            
            while (true) {
                List<CellData[]> batch = reader.readRows(0, offset, batchSize);
                if (batch.isEmpty()) break;
                
                totalRead += batch.size();
                offset += batchSize;
            }
            
            assertEquals(1000, totalRead);
        }
    }
}