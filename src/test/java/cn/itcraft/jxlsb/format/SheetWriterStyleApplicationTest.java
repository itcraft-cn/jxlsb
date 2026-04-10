package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.CellData;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class SheetWriterStyleApplicationTest {

    @Test
    void testWriteCellWithStyle() throws IOException {
        StylesWriter stylesWriter = new StylesWriter();
        int dateStyleId = stylesWriter.addDateFormat("mm-dd-yy");
        
        SharedStringsTable sst = new SharedStringsTable();
        SheetWriter sheetWriter = new SheetWriter(sst, stylesWriter);
        
        byte[] sheetData = sheetWriter.writeSheet(
            (row, col) -> {
                if (col == 0) {
                    return CellData.date(System.currentTimeMillis());
                } else {
                    return CellData.number(row * col);
                }
            },
            10, 2
        );
        
        assertNotNull(sheetData);
        assertTrue(sheetData.length > 100);
    }
    
    @Test
    void testDefaultStyleWhenNoStylesWriter() throws IOException {
        SharedStringsTable sst = new SharedStringsTable();
        SheetWriter sheetWriter = new SheetWriter(sst);
        
        byte[] sheetData = sheetWriter.writeSheet(
            (row, col) -> CellData.number(row),
            10, 10
        );
        
        assertNotNull(sheetData);
        assertTrue(sheetData.length > 100);
    }
}