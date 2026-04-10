package cn.itcraft.jxlsb.example;

import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * XLSB读写示例
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class ReadWriteExample {
    
    public static void main(String[] args) throws IOException {
        Path outputFile = Paths.get("example.xlsb");
        
        writeExample(outputFile);
        readExample(outputFile);
    }
    
    private static void writeExample(Path file) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(file)
                .build()) {
            
            writer.writeBatch("SalesData",
                (row, col) -> {
                    switch (col) {
                        case 0:
                            return CellData.text("Product-" + row);
                        case 1:
                            return CellData.number(row * 100.50);
                        case 2:
                            return CellData.date(System.currentTimeMillis());
                        case 3:
                            return CellData.bool(row % 2 == 0);
                        default:
                            return CellData.blank();
                    }
                },
                100, 4);
            
            System.out.println("Written 100 rows to " + file);
        }
    }
    
    private static void readExample(Path file) throws IOException {
        try (XlsbReader reader = XlsbReader.builder()
                .path(file)
                .build()) {
            
            reader.readSheets(sheet -> {
                System.out.println("Reading sheet: " + sheet.getSheetName());
                System.out.println("Rows: " + sheet.getRowCount());
                
                int rowCount = 0;
                for (OffHeapRow row : sheet) {
                    if (rowCount < 5) {
                        System.out.print("Row " + row.getRowIndex() + ": ");
                        for (int i = 0; i < row.getColumnCount(); i++) {
                            OffHeapCell cell = row.getCell(i);
                            System.out.print(formatCell(cell) + " | ");
                        }
                        System.out.println();
                    }
                    rowCount++;
                }
                
                System.out.println("Total rows read: " + rowCount);
                sheet.close();
            });
        }
    }
    
    private static String formatCell(OffHeapCell cell) {
        switch (cell.getType()) {
            case TEXT:
                return cell.getText();
            case NUMBER:
                return String.format("%.2f", cell.getNumber());
            case DATE:
                return String.valueOf(cell.getDate());
            case BOOLEAN:
                return String.valueOf(cell.getBoolean());
            default:
                return "";
        }
    }
}