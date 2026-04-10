package cn.itcraft.jxlsb.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class XlsbWriterStructureTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesValidZipStructure() throws IOException {
        Path file = tempDir.resolve("test.xlsb");

        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> {
                if (row == 0 && col == 0) return CellData.text("Hello");
                if (row == 0 && col == 1) return CellData.number(123.45);
                return CellData.blank();
            }, 10, 2);
        }

        assertTrue(Files.exists(file));
        System.out.println("File size: " + Files.size(file) + " bytes");

        try (ZipFile zf = new ZipFile(file.toFile())) {
            System.out.println("ZIP entries:");
            zf.entries().asIterator().forEachRemaining(e -> 
                System.out.println("  " + e.getName() + " (" + e.getSize() + " bytes)"));
            
            assertNotNull(zf.getEntry("[Content_Types].xml"), "Missing Content_Types");
            assertNotNull(zf.getEntry("_rels/.rels"), "Missing root rels");
            assertNotNull(zf.getEntry("xl/workbook.bin"), "Missing workbook.bin");
            assertNotNull(zf.getEntry("xl/worksheets/sheet1.bin"), "Missing sheet1.bin");
            assertNotNull(zf.getEntry("xl/_rels/workbook.bin.rels"), "Missing workbook rels");
        }
    }
}