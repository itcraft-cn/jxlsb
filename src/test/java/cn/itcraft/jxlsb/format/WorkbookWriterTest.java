package cn.itcraft.jxlsb.format;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbookWriterTest {
    
    @Test
    void writesWorkbookWithOneSheet() throws Exception {
        WorkbookWriter writer = new WorkbookWriter();
        writer.addSheet("Sheet1");
        
        byte[] data = writer.toBiff12Bytes();
        assertTrue(data.length > 0);
        
        int type = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8);
        assertEquals(Biff12Constants.BEGIN_BOOK, type);
    }
}