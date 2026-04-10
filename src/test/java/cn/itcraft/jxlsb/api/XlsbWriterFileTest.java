package cn.itcraft.jxlsb.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class XlsbWriterFileTest {

    @TempDir
    Path tempDir;

    @Test
    void writeSmallFileProducesNonZeroSize() throws IOException {
        Path file = tempDir.resolve("test.xlsb");

        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1", (row, col) -> CellData.number(row * 100.0 + col), 100, 5);
        }

        long fileSize = Files.size(file);
        System.out.println("Generated file size: " + fileSize + " bytes");

        assertTrue(fileSize > 0, "File should have non-zero size after writing");
    }

    @Test
    void writeMediumFileProducesReasonableSize() throws IOException {
        Path file = tempDir.resolve("test-medium.xlsb");

        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.writeBatch("Sheet1",
                (row, col) -> {
                    switch (col % 4) {
                        case 0: return CellData.text("Data-" + row + "-" + col);
                        case 1: return CellData.number(row * 100.50 + col);
                        case 2: return CellData.bool(row % 2 == 0);
                        default: return CellData.blank();
                    }
                },
                10_000, 10);
        }

        long fileSize = Files.size(file);
        System.out.println("Generated medium file size: " + fileSize + " bytes (" + (fileSize / 1024) + " KB)");

        assertTrue(fileSize > 1024, "Medium file should be at least 1KB");
    }
}