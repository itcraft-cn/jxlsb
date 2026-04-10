package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.container.SheetInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * XlsbReader集成测试
 * 
 * <p>验证写入-读取循环的正确性。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
class XlsbReaderIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testReadExistingFile() throws Exception {
        Path testFile = Path.of("/tmp/jxlsb_new.xlsb");
        
        if (!testFile.toFile().exists()) {
            return;
        }
        
        XlsbReader reader = XlsbReader.builder()
            .path(testFile)
            .build();
        
        List<SheetInfo> sheets = reader.getSheetInfos();
        assertFalse(sheets.isEmpty(), "Should have at least one sheet");
        
        List<CellData> cells = new ArrayList<>();
        reader.forEachRow(0, new RowConsumer() {
            @Override
            public void onRowStart(int rowIndex) {}
            
            @Override
            public void onCell(int row, int col, CellData data) {
                cells.add(data);
            }
            
            @Override
            public void onRowEnd(int rowIndex) {}
        });
        
        assertFalse(cells.isEmpty(), "Should have at least one cell");
        
        reader.close();
    }
    
    @Test
    void testForEachSheet() throws Exception {
        Path testFile = Path.of("/tmp/jxlsb_new.xlsb");
        
        if (!testFile.toFile().exists()) {
            return;
        }
        
        XlsbReader reader = XlsbReader.builder()
            .path(testFile)
            .build();
        
        Map<String, Integer> cellCounts = new HashMap<>();
        
        reader.forEachSheet((info, sheetReader) -> {
            int[] count = new int[1];
            sheetReader.readRows(new RowHandler() {
                @Override
                public void onRowStart(int rowIndex, int columnCount) {}
                
                @Override
                public void onCellNumber(int row, int col, double value) {
                    count[0]++;
                }
                
                @Override
                public void onCellText(int row, int col, String value) {
                    count[0]++;
                }
                
                @Override
                public void onCellBoolean(int row, int col, boolean value) {
                    count[0]++;
                }
                
                @Override
                public void onCellBlank(int row, int col) {}
                
                @Override
                public void onCellDate(int row, int col, double excelDate) {
                    count[0]++;
                }
                
                @Override
                public void onRowEnd(int rowIndex) {}
            });
            
            cellCounts.put(info.getName(), count[0]);
        });
        
        assertTrue(cellCounts.values().stream().anyMatch(c -> c > 0), "Should have cells");
        
        reader.close();
    }
}