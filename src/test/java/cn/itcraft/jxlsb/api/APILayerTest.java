package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("API Layer Test")
class APILayerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should build XlsbReader")
    void shouldBuildXlsbReader() throws IOException {
        Path testFile = tempDir.resolve("test.xlsb");
        testFile.toFile().createNewFile();
        
        try (XlsbReader reader = XlsbReader.builder()
                .path(testFile)
                .build()) {
            assertNotNull(reader);
        }
    }
    
    @Test
    @DisplayName("Should build XlsbWriter")
    void shouldBuildXlsbWriter() throws IOException {
        Path testFile = tempDir.resolve("output.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(testFile)
                .build()) {
            assertNotNull(writer);
        }
    }
    
    @Test
    @DisplayName("Should create sheet with XlsbWriter")
    void shouldCreateSheetWithWriter() throws IOException {
        Path testFile = tempDir.resolve("sheet.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(testFile)
                .build()) {
            
            writer.writeBatch("Sheet1", 
                (row, col) -> CellData.number(row * 100.0 + col),
                10, 5);
            
            assertEquals(1, writer.getSheetCount());
        }
    }
    
    @Test
    @DisplayName("Should write different cell types")
    void shouldWriteDifferentCellTypes() throws IOException {
        Path testFile = tempDir.resolve("cells.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(testFile)
                .build()) {
            
            writer.writeBatch("MixedTypes",
                (row, col) -> {
                    switch (col % 4) {
                        case 0: return CellData.text("Row" + row);
                        case 1: return CellData.number(row * 1.5);
                        case 2: return CellData.date(System.currentTimeMillis());
                        case 3: return CellData.bool(row % 2 == 0);
                        default: return CellData.blank();
                    }
                },
                5, 4);
        }
    }
}