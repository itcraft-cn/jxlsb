package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

class SimpleWatermarkTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testHeaderFooterOnly() throws Exception {
        Path file = Paths.get("/disk2/helly_data/code/ai_cli_gen/jxlsb/watermark_header.xlsb");
        
        Watermark watermark = Watermark.builder()
            .text("机密文件 - 内部资料")
            .type(Watermark.WatermarkType.HEADER_FOOTER)
            .position(Watermark.WatermarkPosition.HEADER_CENTER)
            .build();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatchWithWatermark("数据", (row, col) -> {
                if (col == 0) return CellData.number(row);
                return CellData.text("测试数据-" + row + "-" + col);
            }, 20, 5, watermark);
        }
        
        assertTrue(file.toFile().exists());
        System.out.println("✅ 页眉水印文件: " + file);
        System.out.println("   请用WPS打开 → 打印预览查看页眉");
    }
    
    @Test 
    void testFooterOnly() throws Exception {
        Path file = Paths.get("/disk2/helly_data/code/ai_cli_gen/jxlsb/watermark_footer.xlsb");
        
        Watermark watermark = Watermark.builder()
            .text("第&P页 共&N页 - 内部资料")
            .type(Watermark.WatermarkType.HEADER_FOOTER)
            .position(Watermark.WatermarkPosition.FOOTER_CENTER)
            .build();
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatchWithWatermark("数据", (row, col) -> CellData.number(row * col), 30, 4, watermark);
        }
        
        assertTrue(file.toFile().exists());
        System.out.println("✅ 页脚水印文件: " + file);
        System.out.println("   请用WPS打开 → 打印预览查看页脚");
    }
}