package cn.itcraft.jxlsb.api;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.container.SheetInfo;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

class DemoTemplateFillTest {
    
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
    void fillWithFixedPosition() throws IOException {
        Path templatePath = getTemplatePath();
        Path outputPath = Path.of("/tmp/demo_output_fixed.xlsb");
        
        System.out.println("=== 固定位置填充测试 ===");
        System.out.println("模板: src/test/resources/template/demo_template.xlsb");
        
        List<List<Object>> dataList = new ArrayList<>();
        dataList.add(Arrays.asList("张三", "北京", 25, "男"));
        dataList.add(Arrays.asList("李四", "上海", 30, "女"));
        dataList.add(Arrays.asList("王五", "广州", 28, "男"));
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            System.out.println("模板Sheet数: " + writer.getTemplateSheetCount());
            List<SheetInfo> sheets = writer.getTemplateSheetInfos();
            for (SheetInfo info : sheets) {
                System.out.println("  Sheet: " + info.getName());
            }
            
            System.out.println("\n填充位置: I13 (row=12, col=8)");
            System.out.println("填充数据:");
            for (List<Object> row : dataList) {
                System.out.println("  " + row);
            }
            
            writer.fillBatch(0, dataList, 12, 8);
        }
        
        Files.delete(templatePath);
        System.out.println("\n输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("请用WPS打开验证！");
    }
    
    @Test
    void fillWithMarker() throws IOException {
        Path templatePath = getTemplatePath();
        Path outputPath = Path.of("/tmp/demo_output_marker.xlsb");
        
        System.out.println("=== 标记查找填充测试 ===");
        System.out.println("模板: src/test/resources/template/demo_template.xlsb");
        
        List<List<Object>> dataList = new ArrayList<>();
        dataList.add(Arrays.asList("赵六", "深圳", 35, "男"));
        dataList.add(Arrays.asList("钱七", "杭州", 22, "女"));
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            System.out.println("\n查找标记: ${data}");
            System.out.println("填充数据:");
            for (List<Object> row : dataList) {
                System.out.println("  " + row);
            }
            
            writer.fillAtMarker("${data}", dataList);
        }
        
        Files.delete(templatePath);
        System.out.println("\n输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("请用WPS打开验证！");
    }
    
    @Test
    void streamingFill() throws IOException {
        Path templatePath = getTemplatePath();
        Path outputPath = Path.of("/tmp/demo_output_streaming.xlsb");
        
        System.out.println("=== 流式填充测试 ===");
        System.out.println("模板: src/test/resources/template/demo_template.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .template(templatePath)
                .path(outputPath)
                .build()) {
            
            writer.startFill(0, 12, 8);
            
            writer.fillRows(Arrays.asList(Arrays.asList("孙八", "成都", 40, "男")));
            writer.fillRows(Arrays.asList(Arrays.asList("周九", "武汉", 33, "女")));
            writer.fillRows(Arrays.asList(Arrays.asList("吴十", "南京", 27, "男")));
            
            writer.endFill();
        }
        
        Files.delete(templatePath);
        System.out.println("\n输出文件: " + outputPath + " (" + Files.size(outputPath) + " bytes)");
        System.out.println("请用WPS打开验证！");
    }
}