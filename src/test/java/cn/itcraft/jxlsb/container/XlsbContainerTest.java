package cn.itcraft.jxlsb.container;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class XlsbContainerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void createContainerProducesValidZip() throws Exception {
        Path xlsbFile = tempDir.resolve("test.xlsb");
        
        try (XlsbContainer container = XlsbContainer.create(xlsbFile)) {
            container.addEntry("test.txt", "Hello".getBytes());
        }
        
        assertTrue(Files.exists(xlsbFile));
        assertTrue(Files.size(xlsbFile) > 0);
        
        try (ZipFile zf = new ZipFile(xlsbFile.toFile())) {
            assertNotNull(zf.getEntry("test.txt"));
        }
    }
    
    @Test
    void addMultipleEntries() throws Exception {
        Path xlsbFile = tempDir.resolve("multi.xlsb");
        
        try (XlsbContainer container = XlsbContainer.create(xlsbFile)) {
            container.addEntry("file1.bin", new byte[]{1, 2, 3});
            container.addEntry("dir/file2.bin", new byte[]{4, 5, 6});
        }
        
        try (ZipFile zf = new ZipFile(xlsbFile.toFile())) {
            assertNotNull(zf.getEntry("file1.bin"));
            assertNotNull(zf.getEntry("dir/file2.bin"));
        }
    }
}