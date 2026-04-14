package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.container.SheetInfo;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;

class TemplateFillTest {
    
    @Test
    void createTemplate() throws IOException {
        Path templatePath = Path.of("/tmp/template.xlsb");
        
        System.out.println("=== 创建模板文件 ===");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(templatePath).build()) {
            writer.writeBatch("Template", (row, col) -> {
                if (row == 0) {
                    switch (col) {
                        case 0: return CellData.text("ID");
                        case 1: return CellData.text("Name");
                        case 2: return CellData.text("Amount");
                        case 3: return CellData.text("${data}");
                        default: return CellData.blank();
                    }
                }
                return CellData.blank();
            }, 1, 4);
        }
        
        System.out.println("模板文件: " + templatePath + " (" + Files.size(templatePath) + " bytes)");
        System.out.println("模板结构: Sheet1, 4列, 包含标记 ${data} 在(0,3)");
    }
    
    @Test
    void fillTemplateWithFixedPosition() throws IOException {
        Path templatePath = Path.of("/tmp/template.xlsb");
        Path outputPath = Path.of("/tmp/output_fixed.xlsb");
        
        createTemplate();
        
        System.out.println("\n=== 固定位置填充测试 ===");
        
        List<Object> data = Arrays.asList(
            Arrays.asList(1, "Alice", 100.5),
            Arrays.asList(2, "Bob", 200.0),
            Arrays.asList(3, "Charlie", 300.75)
        );
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            List<SheetInfo> sheets = writer.getTemplateSheetInfos();
            System.out.println("模板Sheet数量: " + writer.getTemplateSheetCount());
            for (SheetInfo info : sheets) {
                System.out.println("  Sheet: " + info.getName());
            }
            
            writer.fillBatch(0, data, 1, 0);
        }
        
        System.out.println("输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("填充位置: row=1, col=0 (从标题下一行开始)");
    }
    
    @Test
    void fillTemplateWithMarker() throws IOException {
        Path templatePath = Path.of("/tmp/template.xlsb");
        Path outputPath = Path.of("/tmp/output_marker.xlsb");
        
        createTemplate();
        
        System.out.println("\n=== 标记查找填充测试 ===");
        
        List<Object> data = Arrays.asList(
            Arrays.asList(10, "David", 150.0),
            Arrays.asList(20, "Eve", 250.25)
        );
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            writer.fillAtMarker("${data}", data);
        }
        
        System.out.println("输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("标记位置: 查找到 ${data} 并从此处填充");
    }
    
    @Test
    void streamingFillTest() throws IOException {
        Path templatePath = Path.of("/tmp/template.xlsb");
        Path outputPath = Path.of("/tmp/output_streaming.xlsb");
        
        createTemplate();
        
        System.out.println("\n=== 流式填充测试 ===");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            writer.startFill(0, 1, 0);
            
            writer.fillRows(Arrays.asList(Arrays.asList(100, "Batch1-A", 1000)));
            writer.fillRows(Arrays.asList(Arrays.asList(200, "Batch1-B", 2000)));
            writer.fillRows(Arrays.asList(Arrays.asList(300, "Batch1-C", 3000)));
            
            writer.endFill();
        }
        
        System.out.println("输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("流式填充: 3批次数据");
    }
}