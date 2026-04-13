package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import static org.junit.jupiter.api.Assertions.*;

class CreatorTagTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void verifyCreatorTag() throws Exception {
        Path file = tempDir.resolve("creator_test.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Test", (row, col) -> CellData.number(row), 10, 5);
        }
        
        try (ZipFile zf = new ZipFile(file.toFile())) {
            ZipEntry coreEntry = zf.getEntry("docProps/core.xml");
            assertNotNull(coreEntry, "core.xml should exist");
            
            String coreContent = new String(zf.getInputStream(coreEntry).readAllBytes());
            
            // 作者 = jxlsb
            assertTrue(coreContent.contains("<dc:creator>jxlsb</dc:creator>"),
                "core.xml should contain creator tag with 'jxlsb'");
            
            // 最后修改者 = jxlsb
            assertTrue(coreContent.contains("<cp:lastModifiedBy>jxlsb</cp:lastModifiedBy>"),
                "core.xml should contain lastModifiedBy tag");
            
            // 备注 = created by jxlsb
            assertTrue(coreContent.contains("<dc:description>created by jxlsb</dc:description>"),
                "core.xml should contain description tag");
            
            ZipEntry appEntry = zf.getEntry("docProps/app.xml");
            assertNotNull(appEntry, "app.xml should exist");
            
            String appContent = new String(zf.getInputStream(appEntry).readAllBytes());
            assertTrue(appContent.contains("<Application>jxlsb</Application>"),
                "app.xml should contain Application tag");
        }
        
        System.out.println("✅ 作者: jxlsb");
        System.out.println("✅ 备注: created by jxlsb");
    }
}