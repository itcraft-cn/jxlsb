package cn.itcraft.jxlsb.examples;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * 基础写入示例
 * 
 * 演示jxlsb的基本写入用法，包括：
 * - 创建XlsbWriter
 * - 使用writeBatch批量写入
 * - 支持的单元格类型
 * 
 * 运行后生成output.xlsb文件，可用Excel/WPS打开
 */
public class BasicWriteExample {
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== jxlsb 基础写入示例 ===\n");
        
        // 输出文件路径
        Path outputPath = Paths.get("basic-output.xlsb");
        
        // 使用Builder创建Writer（推荐try-with-resources）
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(outputPath)
                .build()) {
            
            System.out.println("开始写入数据...");
            
            // 批量写入1000行10列数据
            writer.writeBatch("示例数据", 
                (row, col) -> {
                    // 数据供应函数：根据row和col返回不同类型的数据
                    switch (col % 5) {
                        case 0: 
                            // 第0列：文本
                            return CellData.text("产品-" + row);
                            
                        case 1: 
                            // 第1列：数字（整数）
                            return CellData.number(row * 100);
                            
                        case 2: 
                            // 第2列：数字（小数）
                            return CellData.number(row * 100.5 + col);
                            
                        case 3: 
                            // 第3列：日期（当前时间戳）
                            return CellData.date(System.currentTimeMillis());
                            
                        case 4: 
                            // 第4列：布尔值
                            return CellData.bool(row % 2 == 0);
                            
                        default:
                            // 其他：空白单元格
                            return CellData.blank();
                    }
                },
                1000,  // 行数
                10     // 列数
            );
            
            System.out.println("写入完成！");
        } // 自动调用close()，释放堆外内存
        
        // 检查文件大小
        long fileSize = java.nio.file.Files.size(outputPath);
        System.out.println("\n文件信息：");
        System.out.println("  文件路径: " + outputPath);
        System.out.println("  文件大小: " + (fileSize / 1024) + " KB");
        System.out.println("  数据行数: 1000");
        System.out.println("  数据列数: 10");
        
        System.out.println("\n提示：可以用Excel或WPS打开 basic-output.xlsb 查看内容");
    }
}