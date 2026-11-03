package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.api.LimitExceededException;
import cn.itcraft.jxlsb.api.XlsbWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 与 rxlsb dev 变更对齐的格式/行为测试：跳过空白、流式维度回填、行列上限。
 */
class FormatSyncTest {

    private static byte[] readSheet(Path file) throws Exception {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("xl/worksheets/sheet1.bin");
            try (InputStream in = zip.getInputStream(entry)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                return out.toByteArray();
            }
        }
    }

    private static int findDim(byte[] data) {
        for (int i = 0; i + 19 <= data.length; i++) {
            if ((data[i] & 0xFF) == 0x94 && (data[i + 1] & 0xFF) == 0x01
                    && (data[i + 2] & 0xFF) == 0x10) {
                return i;
            }
        }
        return -1;
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    @Test
    void streamingBackfillsDimension(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("stream.xlsb");
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.startSheet("s", 3);
            for (int batch = 0; batch < 3; batch++) {
                final int base = batch * 100;
                writer.writeRows(java.util.Collections.nCopies(100, "r"), base, (item, col) -> {
                    if (col == 0) {
                        return CellData.number(base);
                    }
                    if (col == 1) {
                        return CellData.text("x");
                    }
                    return CellData.bool(true);
                });
            }
            writer.endSheet();
        }

        byte[] sheet = readSheet(file);
        int dim = findDim(sheet);
        assertTrue(dim > 0, "BrtWsDim 应存在");
        assertEquals(0, readInt(sheet, dim + 3));
        assertEquals(299, readInt(sheet, dim + 7));
        assertEquals(0, readInt(sheet, dim + 11));
        assertEquals(2, readInt(sheet, dim + 15));
    }

    @Test
    void blankCellsAreSkipped(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("blank.xlsb");
        try (XlsbWriter writer = XlsbWriter.builder().path(file).build()) {
            writer.startSheet("s", 2);
            writer.writeRows(java.util.Collections.nCopies(1, "r"), 0,
                    (item, col) -> CellData.blank());
            writer.endSheet();
        }
        byte[] sheet = readSheet(file);
        assertTrue(!hasRecordType(sheet, 1), "不应写入 BrtCellBlank 记录");
    }

    private static boolean hasRecordType(byte[] data, int wanted) {
        int pos = 0;
        while (pos + 2 <= data.length) {
            int type = 0;
            int shift = 0;
            while (pos < data.length) {
                int b = data[pos++] & 0xFF;
                type |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    break;
                }
                shift += 7;
            }
            int size = 0;
            shift = 0;
            while (pos < data.length) {
                int b = data[pos++] & 0xFF;
                size |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    break;
                }
                shift += 7;
            }
            if (type == wanted) {
                return true;
            }
            pos += size;
        }
        return false;
    }

    @Test
    void limitsHonorErrorAndCut(@TempDir Path dir) throws Exception {
        Limits.setOverflow(Limits.OverflowBehavior.ERROR, Limits.OverflowBehavior.ERROR);
        assertThrows(LimitExceededException.class,
                () -> SheetLimits.begin(Limits.MAX_COLUMNS + 1));

        Limits.setOverflow(Limits.OverflowBehavior.CUT, Limits.OverflowBehavior.CUT);
        assertEquals(Limits.MAX_COLUMNS, SheetLimits.begin(Limits.MAX_COLUMNS + 100).columns());
        assertEquals(0, SheetLimits.begin(1).rows(Limits.MAX_ROWS, 10));

        Limits.setOverflow(Limits.OverflowBehavior.ERROR, Limits.OverflowBehavior.ERROR);
    }
}
