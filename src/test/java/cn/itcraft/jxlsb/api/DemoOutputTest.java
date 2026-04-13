package cn.itcraft.jxlsb.api;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

class DemoOutputTest {
    
    @Test
    void generateDemoFile() throws Exception {
        Path file = Paths.get("/disk2/helly_data/code/ai_cli_gen/jxlsb/demo_output.xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("数据", (row, col) -> {
                switch (col % 5) {
                    case 0: return CellData.number(row * 1000 + col);
                    case 1: return CellData.text("产品-" + row);
                    case 2: return CellData.number(row * 1.5);
                    case 3: return CellData.text("类别-" + (row % 10));
                    case 4: return CellData.date(System.currentTimeMillis() - row * 86400000L);
                    default: return CellData.blank();
                }
            }, 100, 10);
            
            writer.startSheet("混合", 4);
            class DemoRow {
                String text;
                double value;
                boolean flag;
                long time;
                DemoRow(String t, double v, boolean f, long tm) {
                    text = t; value = v; flag = f; time = tm;
                }
            }
            writer.writeRows(Arrays.asList(
                new DemoRow("文本", 123.45, true, System.currentTimeMillis()),
                new DemoRow("测试行", 999.99, false, System.currentTimeMillis())
            ), 0, (row, col) -> {
                switch (col) {
                    case 0: return CellData.text(row.text);
                    case 1: return CellData.number(row.value);
                    case 2: return CellData.bool(row.flag);
                    case 3: return CellData.date(row.time);
                    default: return CellData.blank();
                }
            });
            writer.endSheet();
        }
        
        System.out.println("\n✅ 文件生成成功!");
        System.out.println("   路径: " + file);
        System.out.println("   大小: " + file.toFile().length() / 1024 + " KB");
        System.out.println("\n   请用 Excel/WPS 打开查看，右键属性可看到 'created by jxlsb'");
    }
}