package cn.itcraft.jxlsb.exception;

/**
 * XLSB库基础异常
 * 
 * <p>所有jxlsb库异常的基类，提供统一的异常处理接口。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
public class XlsbException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public XlsbException(String message) {
        super(message);
    }
    
    public XlsbException(String message, Throwable cause) {
        super(message, cause);
    }
}