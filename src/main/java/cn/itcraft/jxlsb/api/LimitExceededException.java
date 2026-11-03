package cn.itcraft.jxlsb.api;

/**
 * 写入维度超出 Excel 上限时抛出（{@link cn.itcraft.jxlsb.format.Limits} 的 Error 策略）。
 *
 * @author AI架构师
 * @since 1.2.0
 */
public final class LimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String dimension;
    private final long requested;
    private final long limit;

    public LimitExceededException(String dimension, long requested, long limit) {
        super(dimension + " exceeded: requested " + requested + ", limit " + limit);
        this.dimension = dimension;
        this.requested = requested;
        this.limit = limit;
    }

    public String getDimension() {
        return dimension;
    }

    public long getRequested() {
        return requested;
    }

    public long getLimit() {
        return limit;
    }
}
