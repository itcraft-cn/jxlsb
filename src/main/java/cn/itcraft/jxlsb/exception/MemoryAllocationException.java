package cn.itcraft.jxlsb.exception;

/**
 * 内存分配异常
 * 
 * <p>当堆外内存分配失败时抛出，包含请求的大小信息。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public final class MemoryAllocationException extends XlsbException {
    
    private static final long serialVersionUID = 1L;
    
    private final long requestedSize;
    
    public MemoryAllocationException(long requestedSize, String message) {
        super(String.format("Failed to allocate %d bytes: %s", requestedSize, message));
        this.requestedSize = requestedSize;
    }
    
    public MemoryAllocationException(long requestedSize, String message, Throwable cause) {
        super(String.format("Failed to allocate %d bytes: %s", requestedSize, message), cause);
        this.requestedSize = requestedSize;
    }
    
    public long getRequestedSize() {
        return requestedSize;
    }
}