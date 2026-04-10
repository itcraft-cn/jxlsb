package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NumberFormatRegistryTest {

    @Test
    void testBuiltInDateFormats() {
        NumberFormatRegistry registry = new NumberFormatRegistry();
        
        assertEquals(14, registry.getFormatId("mm-dd-yy"));
        assertEquals(15, registry.getFormatId("d-mmm-yy"));
        assertEquals(16, registry.getFormatId("d-mmm"));
        assertEquals(17, registry.getFormatId("mmm-yy"));
        assertEquals(22, registry.getFormatId("m/d/yy h:mm"));
    }
    
    @Test
    void testAddCustomFormat() {
        NumberFormatRegistry registry = new NumberFormatRegistry();
        
        int customId = registry.addFormat("yyyy-mm-dd");
        assertTrue(customId >= 164);
        
        int retrievedId = registry.getFormatId("yyyy-mm-dd");
        assertEquals(customId, retrievedId);
    }
    
    @Test
    void testDuplicateCustomFormat() {
        NumberFormatRegistry registry = new NumberFormatRegistry();
        
        int id1 = registry.addFormat("#,##0.00");
        int id2 = registry.addFormat("#,##0.00");
        
        assertEquals(id1, id2);
    }
    
    @Test
    void testGetFormatIdNotFound() {
        NumberFormatRegistry registry = new NumberFormatRegistry();
        
        assertEquals(-1, registry.getFormatId("non-existent-format"));
    }
}