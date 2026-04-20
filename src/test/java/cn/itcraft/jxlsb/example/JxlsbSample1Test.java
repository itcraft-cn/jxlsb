package cn.itcraft.jxlsb.example;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.RowConsumer;
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
public class JxlsbSample1Test {

    private static final Logger LOGGER = LoggerFactory.getLogger(JxlsbSample1Test.class);

    private static final Path T1 = Paths.get("/tmp/t1.xlsb");

    @Test
    public void test() {
        List<Product> products = Product.createTable(1000000);
        try (XlsbWriter writer = XlsbWriter.builder().path(T1).build()) {
            final List<Product> data = products;
            writer.writeBatch("Products",
                              (row, col) -> Product.product2Cell(data.get(row), col),
                              products.size(), 5);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (XlsbReader reader = XlsbReader.builder().path(T1).build()) {
            // 场景1: 流式处理（推荐 forEachRow）
            reader.forEachRow(0, new ProductRowConsumer());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.delete(T1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ProductRowConsumer implements RowConsumer {
        @Override
        public void onRowStart(int rowIndex) {
        }

        @Override
        public void onCell(int row, int col, CellData data) {
            // 直接处理，无需存储
            LOGGER.info("row: {}, col: {}, value: {}", row, col, Product.parseCell(data));
        }

        @Override
        public void onRowEnd(int rowIndex) {
        }
    }

}
