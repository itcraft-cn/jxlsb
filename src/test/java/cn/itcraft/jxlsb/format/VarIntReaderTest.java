package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VarInt读取测试
 */
class VarIntReaderTest {
    
    @Test
    void testReadWorkbook() throws Exception {
        Path testFile = Path.of("/tmp/jxlsb_new.xlsb");
        if (!testFile.toFile().exists()) {
            return;
        }
        
        try (InputStream is = new FileInputStream(testFile.toFile())) {
            java.util.zip.ZipFile zip = new java.util.zip.ZipFile(testFile.toFile());
            java.util.zip.ZipEntry entry = zip.getEntry("xl/workbook.bin");
            
            if (entry != null) {
                InputStream wis = zip.getInputStream(entry);
                WorkbookReader reader = new WorkbookReader(wis);
                List<cn.itcraft.jxlsb.container.SheetInfo> sheets = reader.parseSheetList();
                
                System.out.println("Found " + sheets.size() + " sheets");
                for (cn.itcraft.jxlsb.container.SheetInfo info : sheets) {
                    System.out.println("  - " + info.getName());
                }
                
                assertTrue(sheets.size() > 0, "Should find at least one sheet");
            }
        }
    }
}