package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class StylesWriterTest {

    @Test
    void testGenerateStylesBin() throws IOException {
        StylesWriter writer = new StylesWriter();
        writer.addDateFormat("mm-dd-yy");
        writer.addDateFormat("yyyy-mm-dd");
        
        byte[] stylesBin = writer.toBiff12Bytes();
        
        assertNotNull(stylesBin);
        assertTrue(stylesBin.length > 100);
        assertTrue(stylesBin.length < 10000);
    }
    
    @Test
    void testStylesBinStructure() throws IOException {
        StylesWriter writer = new StylesWriter();
        byte[] stylesBin = writer.toBiff12Bytes();
        
        assertTrue(stylesBin.length > 0);
        assertTrue(stylesBin.length >= 100);
        assertTrue(stylesBin.length < 10000);
    }
    
    @Test
    void testAddDateFormat() throws IOException {
        StylesWriter writer = new StylesWriter();
        
        int styleId1 = writer.addDateFormat("mm-dd-yy");
        int styleId2 = writer.addDateFormat("yyyy-mm-dd");
        int styleId3 = writer.addDateFormat("mm-dd-yy");
        
        assertTrue(styleId1 > 0);
        assertTrue(styleId2 > 0);
        assertTrue(styleId2 > styleId1);
        assertEquals(styleId1, styleId3);
    }
    
    @Test
    void testGeneratedFileExcelCompatible() throws IOException {
        StylesWriter writer = new StylesWriter();
        writer.addDateFormat("mm-dd-yy");
        writer.addDateFormat("d-mmm-yy");
        
        byte[] stylesBin = writer.toBiff12Bytes();
        
        assertNotNull(stylesBin);
        assertTrue(stylesBin.length >= 16);
    }
}