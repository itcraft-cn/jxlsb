package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.container.SheetInfo;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

class TemplateFillTest {
    
    private Path getTemplatePath() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("template/demo_template.xlsb");
        if (is == null) {
            throw new IOException("Template not found: template/demo_template.xlsb");
        }
        Path tempTemplate = Files.createTempFile("demo_template_", ".xlsb");
        Files.copy(is, tempTemplate, StandardCopyOption.REPLACE_EXISTING);
        is.close();
        return tempTemplate;
    }
    
    @Test
    void fillTemplateWithFixedPosition() throws IOException {
        Path templatePath = getTemplatePath();
        Path outputPath = Paths.get("/tmp/output_fixed.xlsb");
        
        System.out.println("=== 固定位置填充测试 ===");
        System.out.println("模板: src/test/resources/template/demo_template.xlsb");
        
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
            
            writer.fillBatch(0, data, 12, 8);
        }
        
        Files.delete(templatePath);
        System.out.println("输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("填充位置: row=12, col=8 (I13位置)");
    }
    
    @Test
    void fillTemplateWithMarker() throws IOException {
        Path templatePath = getTemplatePath();
        Path outputPath = Paths.get("/tmp/output_marker.xlsb");
        
        System.out.println("\n=== 标记查找填充测试 ===");
        System.out.println("模板: src/test/resources/template/demo_template.xlsb");
        
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
        
        Files.delete(templatePath);
        System.out.println("输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("标记位置: 查找到 ${data} 并从此处填充");
    }
    
    @Test
    void streamingFillTest() throws IOException {
        Path templatePath = getTemplatePath();
        Path outputPath = Paths.get("/tmp/output_streaming.xlsb");
        
        System.out.println("\n=== 流式填充测试 ===");
        System.out.println("模板: src/test/resources/template/demo_template.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            writer.startFill(0, 12, 8);
            
            writer.fillRows(Arrays.asList(Arrays.asList(100, "Batch1-A", 1000)));
            writer.fillRows(Arrays.asList(Arrays.asList(200, "Batch1-B", 2000)));
            writer.fillRows(Arrays.asList(Arrays.asList(300, "Batch1-C", 3000)));
            
            writer.endFill();
        }
        
        Files.delete(templatePath);
        System.out.println("输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("流式填充: 3批次数据");
    }
}