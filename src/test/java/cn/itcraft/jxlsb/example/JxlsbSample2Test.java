package cn.itcraft.jxlsb.example;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.RowConsumer;
import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.api.XlsbWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    public void testSmallBatch() {
        List<Product> products = Product.createTable(100);
        try (XlsbWriter writer = XlsbWriter.builder().path(T2).build()) {
            writer.startSheet("Products", 5);
            int offset = 0;
            for (int i = 0; i < 1000; i++) {
                writer.writeRows(products, offset, Product::product2Cell);
                offset += products.size();
            }
            writer.endSheet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int errorCount = 0;
        try (XlsbReader reader = XlsbReader.builder().path(T2).build()) {
            int offset = 0;
            for (int batchIdx = 0; batchIdx < 1000; batchIdx++) {
                List<CellData[]> batch = reader.readRows(0, offset, 100);
                for (int rowIdx = 0; rowIdx < batch.size(); rowIdx++) {
                    CellData[] row = batch.get(rowIdx);
                    for (int colIdx = 0; colIdx < row.length; colIdx++) {
                        CellData cell = row[colIdx];
                        if (cell == null) {
                            int actualRow = offset + rowIdx;
                            System.err.println("Batch " + batchIdx + " row " + rowIdx + " (actual row " + actualRow + ") col " + colIdx + " is NULL!");
                            errorCount++;
                        }
                    }
                }
                offset += 100;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (errorCount > 0) {
            throw new AssertionError("Found " + errorCount + " null cells");
        }
        try {
            Files.delete(T2);
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testSingleBatchRead() {
        List<Product> products = Product.createTable(10000);
        try (XlsbWriter writer = XlsbWriter.builder().path(T2).build()) {
            writer.startSheet("Products", 5);
            writer.writeRows(products, 0, Product::product2Cell);
            writer.endSheet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (XlsbReader reader = XlsbReader.builder().path(T2).build()) {
            List<CellData[]> batch = reader.readRows(0, 0, 10000);
            assertEquals(10000, batch.size());
            int firstNullRow = -1;
            int firstNullCol = -1;
            for (int rowIdx = 0; rowIdx < batch.size(); rowIdx++) {
                CellData[] row = batch.get(rowIdx);
                assertEquals(5, row.length);
                for (int colIdx = 0; colIdx < row.length; colIdx++) {
                    if (row[colIdx] == null) {
                        if (firstNullRow < 0) {
                            firstNullRow = rowIdx;
                            firstNullCol = colIdx;
                            System.err.println("First null at row " + rowIdx + " col " + colIdx);
                        }
                    }
                }
            }
            if (firstNullRow >= 0) {
                fail("First null cell at row " + firstNullRow + " col " + firstNullCol);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.delete(T2);
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testReadStreamOrder() {
        List<Product> products = Product.createTable(100);
        try (XlsbWriter writer = XlsbWriter.builder().path(T2).build()) {
            writer.startSheet("Products", 5);
            writer.writeRows(products, 0, Product::product2Cell);
            writer.endSheet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final int[] lastRowSeen = {-1};
        final int[] cellsBeforeRowHdr = {0};
        try (XlsbReader reader = XlsbReader.builder().path(T2).build()) {
            reader.forEachRow(0, new RowConsumer() {
                @Override
                public void onRowStart(int rowIndex) {
                    lastRowSeen[0] = rowIndex;
                }
                
                @Override
                public void onCell(int row, int col, CellData data) {
                    if (row != lastRowSeen[0]) {
                        System.err.println("CELL BEFORE ROW HDR! cell row=" + row + " but lastRowHdr=" + lastRowSeen[0]);
                        cellsBeforeRowHdr[0]++;
                    }
                }
                
                @Override
                public void onRowEnd(int rowIndex) {
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (cellsBeforeRowHdr[0] > 0) {
            throw new AssertionError("Found " + cellsBeforeRowHdr[0] + " cells before row header");
        }
        try {
            Files.delete(T2);
        } catch (IOException e) {
            // ignore
        }
    }

    @Test
    public void testForEachRow10000() {
        List<Product> products = Product.createTable(10000);
        try (XlsbWriter writer = XlsbWriter.builder().path(T2).build()) {
            writer.startSheet("Products", 5);
            writer.writeRows(products, 0, Product::product2Cell);
            writer.endSheet();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int rowCount = 0;
        int nullCount = 0;
        try (XlsbReader reader = XlsbReader.builder().path(T2).build()) {
            reader.forEachRow(0, new RowConsumer() {
                int cellsInRow = 0;
                
                @Override
                public void onRowStart(int rowIndex) {
                    cellsInRow = 0;
                }
                
                @Override
                public void onCell(int row, int col, CellData data) {
                    if (data == null) {
                        System.err.println("forEachRow: null at row " + row + " col " + col);
                    }
                    cellsInRow++;
                }
                
                @Override
                public void onRowEnd(int rowIndex) {
                    if (cellsInRow != 5) {
                        System.err.println("forEachRow: row " + rowIndex + " has " + cellsInRow + " cells, expected 5");
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.delete(T2);
        } catch (IOException e) {
            // ignore
        }
    }
}
