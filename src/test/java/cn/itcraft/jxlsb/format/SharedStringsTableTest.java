package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedStringsTableTest {
    
    @Test
    void deduplicatesStrings() {
        SharedStringsTable sst = new SharedStringsTable();
        
        int idx1 = sst.addString("Hello");
        int idx2 = sst.addString("World");
        int idx3 = sst.addString("Hello");
        
        assertEquals(0, idx1);
        assertEquals(1, idx2);
        assertEquals(0, idx3);
        assertEquals(2, sst.getCount());
        assertEquals(3, sst.getTotalCount());
    }
    
    @Test
    void writesToBytes() throws Exception {
        SharedStringsTable sst = new SharedStringsTable();
        sst.addString("A");
        sst.addString("B");
        
        byte[] data = sst.toBiff12Bytes();
        assertTrue(data.length > 0);
    }
}