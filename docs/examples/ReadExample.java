package cn.itcraft.jxlsb.examples;

import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.data.CellType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * 读取示例
 * 
 * 演示jxlsb的读取功能：
 * - 创建测试文件
 * - 流式读取数据
 * - 处理不同类型单元格
 * - 统计数据信息
 */
public class ReadExample {
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== jxlsb 读取示例 ===\n");
        
        // 第一步：创建测试文件
        Path testFile = Paths.get("read-test.xlsb");
        createTestFile(testFile);
        
        System.out.println("测试文件创建完成: " + testFile);
        System.out.println();
        
        // 第二步：读取文件
        System.out.println("开始读取文件...\n");
        
        try (XlsbReader reader = XlsbReader.builder()
                .path(testFile)
                .build()) {
            
            // 统计变量
            int totalRows = 0;
            int totalCells = 0;
            int textCount = 0;
            int numberCount = 0;
            int dateCount = 0;
            int boolCount = 0;
            int blankCount = 0;
            
            // 流式读取所有Sheet
            reader.readSheets(sheet -> {
                System.out.println("Sheet信息：");
                System.out.println("  名称: " + sheet.getSheetName());
                System.out.println("  索引: " + sheet.getSheetIndex());
                System.out.println();
                
                // 遍历行
                for (OffHeapRow row : sheet) {
                    totalRows++;
                    
                    // 遍历单元格
                    for (int i = 0; i < row.getColumnCount(); i++) {
                        OffHeapCell cell = row.getCell(i);
                        totalCells++;
                        
                        // 根据类型统计和处理
                        CellType type = cell.getType();
                        switch (type) {
                            case TEXT:
                                textCount++;
                                // String text = cell.getText();
                                break;
                                
                            case NUMBER:
                                numberCount++;
                                // double number = cell.getNumber();
                                break;
                                
                            case DATE:
                                dateCount++;
                                // long timestamp = cell.getDate();
                                break;
                                
                            case BOOLEAN:
                                boolCount++;
                                // boolean bool = cell.getBoolean();
                                break;
                                
                            case BLANK:
                                blankCount++;
                                break;
                        }
                    }
                    
                    // 示例：打印前5行数据
                    if (totalRows <= 5) {
                        System.out.println("行 " + (totalRows - 1) + ":");
                        for (int i = 0; i < Math.min(row.getColumnCount(), 5); i++) {
                            OffHeapCell cell = row.getCell(i);
                            System.out.println("  列" + i + ": " + formatCell(cell));
                        }
                    }
                }
            });
            
            // 打印统计信息
            System.out.println("\n数据统计：");
            System.out.println("  总行数: " + totalRows);
            System.out.println("  总单元格: " + totalCells);
            System.out.println("  文本单元格: " + textCount);
            System.out.println("  数字单元格: " + numberCount);
            System.out.println("  日期单元格: " + dateCount);
            System.out.println("  布尔单元格: " + boolCount);
            System.out.println("  空白单元格: " + blankCount);
        } // 自动关闭Reader
        
        // 清理测试文件
        java.nio.file.Files.deleteIfExists(testFile);
        System.out.println("\n测试文件已清理");
    }
    
    /**
     * 创建测试文件
     */
    private static void createTestFile(Path file) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(file)
                .build()) {
            
            writer.writeBatch("测试数据", 
                (row, col) -> {
                    switch (col % 5) {
                        case 0: return CellData.text("文本-" + row);
                        case 1: return CellData.number(row * 100.5);
                        case 2: return CellData.date(System.currentTimeMillis());
                        case 3: return CellData.bool(row % 2 == 0);
                        case 4: return CellData.blank();
                        default: return CellData.number(row);
                    }
                },
                100,  // 100行
                10    // 10列
            );
        }
    }
    
    /**
     * 格式化单元格值用于显示
     */
    private static String formatCell(OffHeapCell cell) {
        CellType type = cell.getType();
        
        switch (type) {
            case TEXT:
                return "\"" + cell.getText() + "\"";
                
            case NUMBER:
                return String.format("%.2f", cell.getNumber());
                
            case DATE:
                long timestamp = cell.getDate();
                java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
                return instant.toString();
                
            case BOOLEAN:
                return cell.getBoolean() ? "true" : "false";
                
            case BLANK:
                return "[空白]";
                
            default:
                return "[未知]";
        }
    }
}