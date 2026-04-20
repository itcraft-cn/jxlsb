package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

class DateFormatTest {
    
    @Test
    void writeDatesWithCorrectFormat() throws Exception {
        Path output = Paths.get("/tmp/date_format_test.xlsb");
        
        System.out.println("=== 日期格式测试 ===\n");
        
        long now = System.currentTimeMillis();
        
        LocalDateTime[] testDates = {
            LocalDateTime.of(2026, 4, 14, 10, 30, 0),
            LocalDateTime.of(2025, 12, 25, 0, 0, 0),
            LocalDateTime.of(2024, 1, 1, 12, 0, 0),
        };
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("Dates", (row, col) -> {
                if (row < testDates.length) {
                    LocalDateTime dt = testDates[row];
                    long timestamp = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    switch (col) {
                        case 0: return CellData.text(dt.toString());
                        case 1: return CellData.date(timestamp);
                        case 2: return CellData.number(row);
                        default: return CellData.blank();
                    }
                }
                return CellData.blank();
            }, testDates.length, 3);
        }
        
        System.out.println("文件已生成: " + output);
        System.out.println("文件大小: " + Files.size(output) + " bytes");
        System.out.println("\n测试日期:");
        for (LocalDateTime dt : testDates) {
            System.out.println("  " + dt);
        }
        System.out.println("\n请用WPS打开，日期列应显示正确日期格式（如 2026/4/14 10:30），而非数字。");
    }
    
    @Test
    void writeMixedTypes() throws Exception {
        Path output = Paths.get("/tmp/mixed_types.xlsb");
        
        System.out.println("=== 混合类型测试 ===\n");
        
        try (XlsbWriter writer = XlsbWriter.builder().path(output).build()) {
            writer.writeBatch("Mixed", (row, col) -> {
                switch (col) {
                    case 0: return CellData.text("Row-" + row);
                    case 1: return CellData.number(row * 100.5);
                    case 2: return CellData.date(System.currentTimeMillis() - row * 3600000L);
                    case 3: return CellData.bool(row % 2 == 0);
                    default: return CellData.blank();
                }
            }, 20, 4);
        }
        
        System.out.println("文件: " + output + " (" + Files.size(output) + " bytes)");
        System.out.println("\n请验证：日期列显示日期格式，数字列显示数字格式。");
    }
}