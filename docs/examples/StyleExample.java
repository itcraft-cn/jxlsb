package cn.itcraft.jxlsb.examples;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 样式示例（演示样式概念）
 * 
 * 说明：
 * - 当前版本样式支持仍在开发中
 * - 此示例展示预期的样式API用法
 * - 样式包括：日期格式、数字格式等
 * 
 * 注意：此示例仅演示API设计，实际功能待实现
 */
public class StyleExample {
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== jxlsb 样式示例 ===\n");
        
        System.out.println("注意：样式功能当前正在开发中");
        System.out.println("此示例演示预期的API用法\n");
        
        // 输出文件路径
        Path outputPath = Paths.get("styled-output.xlsb");
        
        // 当前版本：基础写入（无样式）
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(outputPath)
                .build()) {
            
            System.out.println("写入带日期和数字的数据...");
            
            // 写入数据（当前版本按默认格式显示）
            writer.writeBatch("样式演示", 
                (row, col) -> {
                    switch (col) {
                        case 0:
                            // 产品名称（文本）
                            return CellData.text("产品-" + row);
                            
                        case 1:
                            // 价格（数字）
                            // 注意：当前版本不支持自定义格式，默认显示为数字
                            // 预期功能：支持 "#,##0.00" 格式
                            return CellData.number(row * 100.5);
                            
                        case 2:
                            // 数量（整数）
                            return CellData.number(row * 10);
                            
                        case 3:
                            // 日期（时间戳）
                            // 注意：当前版本不支持日期格式，显示为数字
                            // 预期功能：支持 "yyyy-mm-dd" 格式
                            LocalDateTime dateTime = LocalDateTime.now()
                                .minusDays(row);
                            long timestamp = dateTime
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli();
                            return CellData.date(timestamp);
                            
                        case 4:
                            // 状态（布尔）
                            return CellData.bool(row % 2 == 0);
                            
                        default:
                            return CellData.blank();
                    }
                },
                100,  // 100行
                5     // 5列
            );
            
            System.out.println("写入完成！");
        }
        
        // 检查文件
        long fileSize = java.nio.file.Files.size(outputPath);
        System.out.println("\n文件信息：");
        System.out.println("  文件路径: " + outputPath);
        System.out.println("  文件大小: " + fileSize + " bytes");
        
        System.out.println("\n预期样式功能（待实现）：");
        System.out.println("  - 日期格式：yyyy-mm-dd, mm-dd-yy, d-mmm-yy等");
        System.out.println("  - 数字格式：#,##0.00, 百分比, 科学计数法等");
        System.out.println("  - 字体样式：字体、大小、颜色等");
        System.out.println("  - 边框和填充：边框样式、背景色等");
        
        System.out.println("\n示例代码（预期API）：");
        System.out.println("  // 添加日期格式");
        System.out.println("  int dateStyleId = writer.addDateFormat(\"yyyy-mm-dd\");");
        System.out.println("  ");
        System.out.println("  // 添加数字格式");
        System.out.println("  int numberStyleId = writer.addNumberFormat(\"#,##0.00\");");
        System.out.println("  ");
        System.out.println("  // 应用样式");
        System.out.println("  cell.setNumber(value, numberStyleId);");
        System.out.println("  cell.setDate(timestamp, dateStyleId);");
        
        System.out.println("\n提示：打开 styled-output.xlsb 查看数据");
        System.out.println("      日期列当前显示为数字（Excel序列号）");
    }
}