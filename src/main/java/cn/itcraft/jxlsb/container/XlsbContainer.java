package cn.itcraft.jxlsb.container;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class XlsbContainer implements AutoCloseable {
    
    private final ZipOutputStream zipOut;
    private final OutputStream fileOut;
    
    private XlsbContainer(ZipOutputStream zipOut, OutputStream fileOut) {
        this.zipOut = zipOut;
        this.fileOut = fileOut;
    }
    
    /** 默认压缩级别：速度优先。 */
    public static final int DEFAULT_COMPRESSION_LEVEL = 1;

    public static XlsbContainer create(Path path) throws IOException {
        return create(path, DEFAULT_COMPRESSION_LEVEL);
    }

    /**
     * 创建容器并指定 ZIP 压缩级别（0=store，1-9=deflate）。
     */
    public static XlsbContainer create(Path path, int compressionLevel) throws IOException {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compression level must be in 0..=9, got " + compressionLevel);
        }
        // 使用256KB缓冲区，减少磁盘IO次数
        OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(path), 256 * 1024);
        ZipOutputStream zipOut = new ZipOutputStream(fileOut);
        zipOut.setLevel(compressionLevel);
        return new XlsbContainer(zipOut, fileOut);
    }
    
    public void addEntry(String name, byte[] data) throws IOException {
        zipOut.putNextEntry(new ZipEntry(name));
        zipOut.write(data);
        zipOut.closeEntry();
    }
    
    public void addEntry(String name, byte[] data, int offset, int length) throws IOException {
        zipOut.putNextEntry(new ZipEntry(name));
        zipOut.write(data, offset, length);
        zipOut.closeEntry();
    }
    
    @Override
    public void close() throws IOException {
        IOException primaryException = null;
        try {
            zipOut.finish();
        } catch (IOException e) {
            primaryException = e;
        }
        try {
            zipOut.close();
        } catch (IOException e) {
            if (primaryException == null) {
                primaryException = e;
            }
        }
        try {
            fileOut.close();
        } catch (IOException e) {
            if (primaryException == null) {
                primaryException = e;
            }
        }
        if (primaryException != null) {
            throw primaryException;
        }
    }
}