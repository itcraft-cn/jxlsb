package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VarIntReaderTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testReadWorkbook() throws Exception {
        Path testFile = tempDir.resolve("test.xlsb");
        
        try (cn.itcraft.jxlsb.api.XlsbWriter writer = cn.itcraft.jxlsb.api.XlsbWriter.builder()
                .path(testFile)
                .build()) {
            writer.writeBatch("Sheet1", 
                (row, col) -> cn.itcraft.jxlsb.api.CellData.number(row * 10.0 + col),
                10, 5);
        }
        
        try (InputStream is = new FileInputStream(testFile.toFile())) {
            java.util.zip.ZipFile zip = new java.util.zip.ZipFile(testFile.toFile());
            java.util.zip.ZipEntry entry = zip.getEntry("xl/workbook.bin");
            
            if (entry != null) {
                InputStream wis = zip.getInputStream(entry);
                WorkbookReader reader = new WorkbookReader(wis);
                List<cn.itcraft.jxlsb.container.SheetInfo> sheets = reader.parseSheetList();
                
                assertTrue(sheets.size() > 0, "Should find at least one sheet");
                zip.close();
            }
        }
    }
}