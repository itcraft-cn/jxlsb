package cn.itcraft.jxlsb.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IoUtils {
    
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int FILE_BUFFER_SIZE = 4096;
    
    public static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
    
    public static byte[] readAllBytes(InputStream is, int bufferSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
    
    public static byte[] readFileBytes(InputStream is) throws IOException {
        return readAllBytes(is, FILE_BUFFER_SIZE);
    }
    
    private IoUtils() {}
}