package cn.itcraft.jxlsb.container;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentTypesTest {
    
    @Test
    void generatesValidContentTypesXml() {
        ContentTypes ct = new ContentTypes();
        ct.addOverride("/xl/workbook.bin", "application/vnd.ms-excel.workbook");
        ct.addOverride("/xl/worksheets/sheet1.bin", "application/vnd.ms-excel.worksheet");
        
        byte[] xml = ct.toXml();
        String content = new String(xml);
        
        assertTrue(content.contains("<?xml version"));
        assertTrue(content.contains("Types"));
        assertTrue(content.contains("Override"));
        assertTrue(content.contains("/xl/workbook.bin"));
    }
}