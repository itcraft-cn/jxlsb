package cn.itcraft.jxlsb.memory;

/**
 * 堆外内存块抽象接口
 * 
 * <p>代表一块堆外内存，提供读写操作。
 * 所有数据在堆外内存中操作，堆上仅持有此引用对象。
 * 内存块由MemoryPool管理，支持复用。
 * 
 * <p>实现AutoCloseable接口，确保资源安全释放。
 * 使用小端序（Little-Endian）存储数据，符合XLSB格式要求。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public interface MemoryBlock extends AutoCloseable {
    
    /**
     * 写入字节数据到指定偏移位置
     */
    void putByte(long offset, byte value);
    
    /**
     * 写入short数据（小端序）
     */
    void putShort(long offset, short value);
    
    /**
     * 写入int数据（小端序）
     */
    void putInt(long offset, int value);
    
    /**
     * 写入long数据（小端序）
     */
    void putLong(long offset, long value);
    
    /**
     * 写入double数据（小端序）
     */
    void putDouble(long offset, double value);
    
    /**
     * 写入字节数组到指定偏移位置
     */
    void putBytes(long offset, byte[] src, int srcOffset, int length);
    
    /**
     * 读取指定偏移位置的字节
     */
    byte getByte(long offset);
    
    /**
     * 读取short数据（小端序）
     */
    short getShort(long offset);
    
    /**
     * 读取int数据（小端序）
     */
    int getInt(long offset);
    
    /**
     * 读取long数据（小端序）
     */
    long getLong(long offset);
    
    /**
     * 读取double数据（小端序）
     */
    double getDouble(long offset);
    
    /**
     * 读取字节数组到目标数组
     */
    void getBytes(long offset, byte[] dst, int dstOffset, int length);
    
    /**
     * 获取内存块大小
     */
    long size();
    
    /**
     * 获取内存块基地址
     */
    long getAddress();
    
    /**
     * 释放内存块
     */
    @Override
    void close();
}