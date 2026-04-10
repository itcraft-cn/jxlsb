package cn.itcraft.jxlsb.container;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class XlsbContainer implements AutoCloseable {
    
    private final ZipOutputStream zipOut;
    
    private XlsbContainer(ZipOutputStream zipOut) {
        this.zipOut = zipOut;
    }
    
    public static XlsbContainer create(Path path) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(path.toFile()));
        return new XlsbContainer(zipOut);
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
        zipOut.finish();
        zipOut.close();
    }
}