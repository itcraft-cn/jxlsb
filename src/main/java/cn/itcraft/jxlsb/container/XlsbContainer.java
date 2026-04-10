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
    
    public static XlsbContainer create(Path path) throws IOException {
        // 使用256KB缓冲区，减少磁盘IO次数
        OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(path), 256 * 1024);
        ZipOutputStream zipOut = new ZipOutputStream(fileOut);
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
        try {
            zipOut.finish();
        } finally {
            zipOut.close();
            fileOut.close();
        }
    }
}