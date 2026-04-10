package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CellStyleRegistryTest {

    @Test
    void testDefaultStyle() {
        CellStyleRegistry registry = new CellStyleRegistry();
        
        int defaultId = registry.getDefaultStyleId();
        assertEquals(0, defaultId);
    }
    
    @Test
    void testAddStyle() {
        CellStyleRegistry registry = new CellStyleRegistry();
        
        int styleId = registry.addStyle(14, 0, 0, 0);
        assertTrue(styleId > 0);
    }
    
    @Test
    void testGetDateStyleId() {
        CellStyleRegistry registry = new CellStyleRegistry();
        
        int dateStyleId = registry.getDateStyleId("mm-dd-yy");
        assertTrue(dateStyleId > 0);
        
        int sameId = registry.getDateStyleId("mm-dd-yy");
        assertEquals(dateStyleId, sameId);
    }
    
    @Test
    void testGetDateStyleIdWithCustomFormat() {
        CellStyleRegistry registry = new CellStyleRegistry();
        
        int customDateStyleId = registry.getDateStyleId("yyyy-mm-dd hh:mm:ss");
        assertTrue(customDateStyleId > 0);
    }
}