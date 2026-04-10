package cn.itcraft.jxlsb.container;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelsGeneratorTest {
    
    @Test
    void generatesRootRels() {
        byte[] xml = RelsGenerator.generateRootRels();
        String content = new String(xml);
        
        assertTrue(content.contains("<?xml version"));
        assertTrue(content.contains("Relationships"));
        assertTrue(content.contains("rId1"));
        assertTrue(content.contains("workbook.bin"));
    }
    
    @Test
    void generatesWorkbookRels() {
        byte[] xml = RelsGenerator.generateWorkbookRels(1);
        String content = new String(xml);
        
        assertTrue(content.contains("sheet1.bin"));
        assertTrue(content.contains("rId1"));
    }
}