package cn.itcraft.jxlsb.examples;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * 流式写入示例
 * 
 * 演示如何使用jxlsb处理大规模数据：
 * - 写入100K行数据（约2.6MB文件）
 * - 流式API避免内存溢出
 * - 性能统计
 * 
 * 运行前建议配置：-XX:MaxDirectMemorySize=2g
 */
public class StreamWriteExample {
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== jxlsb 流式写入示例（大数据） ===\n");
        
        // 输出文件路径
        Path outputPath = Paths.get("stream-output.xlsb");
        
        // 数据量配置
        int rowCount = 100_000;  // 10万行
        int colCount = 50;       // 50列
        
        System.out.println("配置信息：");
        System.out.println("  行数: " + rowCount);
        System.out.println("  列数: " + colCount);
        System.out.println("  预计文件大小: ~2.6 MB");
        System.out.println();
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 创建Writer并写入数据
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(outputPath)
                .build()) {
            
            System.out.println("开始写入 " + rowCount + " 行数据...");
            
            // 使用writeBatch流式写入
            // 内存占用稳定，不会随数据量增长
            writer.writeBatch("大数据表", 
                (row, col) -> {
                    // 模拟业务数据
                    if (col % 4 == 0) {
                        // 文本数据
                        return CellData.text("数据-" + row + "-" + col);
                    } else if (col % 4 == 1) {
                        // 数字数据（整数）
                        return CellData.number(row * col);
                    } else if (col % 4 == 2) {
                        // 数字数据（小数）
                        return CellData.number(row * 0.001 + col);
                    } else {
                        // 布尔数据
                        return CellData.bool(row % 2 == 0);
                    }
                },
                rowCount,
                colCount
            );
            
            System.out.println("写入完成！");
        } // 自动关闭Writer
        
        // 统计性能
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long fileSize = java.nio.file.Files.size(outputPath);
        
        System.out.println("\n性能统计：");
        System.out.println("  写入时间: " + duration + " ms");
        System.out.println("  文件大小: " + (fileSize / 1024 / 1024) + " MB");
        System.out.println("  写入速度: " + (fileSize / 1024.0 / duration) + " KB/s");
        System.out.println("  单元格总数: " + (rowCount * colCount));
        System.out.println("  平均每行耗时: " + (duration / rowCount) + " ms");
        
        // 对比参考数据
        System.out.println("\n对比参考（100K行10列）：");
        System.out.println("  jxlsb: ~2.61 MB, 590 ms");
        System.out.println("  POI: ~4.16 MB, 1826 ms");
        System.out.println("  EasyExcel: ~4.18 MB, 1173 ms");
        
        System.out.println("\n提示：可以用Excel或WPS打开 stream-output.xlsb 查看内容");
    }
}