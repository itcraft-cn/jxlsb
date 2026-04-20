package cn.itcraft.jxlsb.example;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.api.XlsbWriter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Helly Guo
 * <p>
 * Created on 2026-04-20 01:04
 */
public class JxlsbSample2Test {

    private static final Logger LOGGER = LoggerFactory.getLogger(JxlsbSample2Test.class);

    private static final Path T2 = Paths.get("/tmp/t2.xlsb");

    @Test
    public void test() {
        List<Product> products = Product.createTable(1000);
        try (XlsbWriter writer = XlsbWriter.builder().path(T2).build()) {
            // 场景2: 模拟数据库分页导出（推荐 writeRows 流式追加）
            writer.startSheet("Products", 5);
            int offset = 0;
            for (int i = 0; i < 100; i++) {
                writer.writeRows(products, offset, Product::product2Cell);
                offset += products.size();
            }
            writer.endSheet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (XlsbReader reader = XlsbReader.builder().path(T2).build()) {
            // 场景2: 分页批量处理（推荐 readRows）
            int offset = 0;
            for (int i = 0; i < 100; i++) {
                List<CellData[]> batch = reader.readRows(0, offset, 1000);
                // 批量处理1000行
                batchProcess(batch, offset);
                offset += 1000;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.delete(T2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void batchProcess(List<CellData[]> batch, int offset) {
        int idx = offset + 1;
        for (CellData[] row : batch) {
            int colIdx = 1;
            for (CellData cell : row) {
                LOGGER.info("row:{}, col:{}, row cols:{}, value: {}",
                            idx, colIdx++, row.length,
                            Product.parseCell(cell));
            }
            idx++;
        }
    }

}
