package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.XlsbWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SheetWriter With Styles Test")
final class SheetWriterWithStylesTest {

    @Test
    @DisplayName("Write date cells with format styles")
    void testWriteDateCellsWithStyles() throws IOException {
        Path path = Files.createTempFile("styled-dates", ".xlsb");
        
        StylesWriter stylesWriter = new StylesWriter();
        int dateStyleId = stylesWriter.addDateFormat("mm-dd-yy");
        
        SharedStringsTable sst = new SharedStringsTable();
        SheetWriter sheetWriter = new SheetWriter(sst);
        
        byte[] sheetBin = sheetWriter.writeSheet(
            (row, col) -> {
                if (col == 0) {
                    return CellData.number(row);
                } else if (col == 1) {
                    return CellData.date(System.currentTimeMillis());
                } else {
                    return CellData.text("Row-" + row);
                }
            },
            10, 3
        );
        
        byte[] stylesBin = stylesWriter.toBiff12Bytes();
        
        assertTrue(sheetBin.length > 100);
        assertTrue(stylesBin.length > 100);
        
        Files.deleteIfExists(path);
    }
    
    @Test
    @DisplayName("Generated file structure is valid")
    void testGeneratedFileStructure() throws IOException {
        StylesWriter stylesWriter = new StylesWriter();
        
        stylesWriter.addDateFormat("mm-dd-yy");
        stylesWriter.addDateFormat("yyyy-mm-dd");
        stylesWriter.addDateFormat("m/d/yy h:mm");
        
        byte[] stylesBin = stylesWriter.toBiff12Bytes();
        
        assertTrue(stylesBin.length > 200);
        assertTrue(stylesBin.length < 10000);
        
        assertNotNull(stylesBin);
    }
}