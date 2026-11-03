package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.LimitExceededException;

/**
 * 一次写入操作内的维度限制快照。
 *
 * <p>在批量写、流式写与模板填充开始时创建一次，之后的写入都通过本对象判断，
 * 保证全局策略的修改“只对设置后的操作生效”。
 *
 * @author AI架构师
 * @since 1.2.0
 */
public final class SheetLimits {

    private final int columns;
    private final Limits.OverflowBehavior rowBehavior;

    private SheetLimits(int columns, Limits.OverflowBehavior rowBehavior) {
        this.columns = columns;
        this.rowBehavior = rowBehavior;
    }

    public static SheetLimits begin(int requestedColumns) {
        if (requestedColumns < 0) {
            throw new IllegalArgumentException("requestedColumns must not be negative");
        }
        int columns = requestedColumns;
        if (columns > Limits.MAX_COLUMNS) {
            if (Limits.getColumnOverflow() == Limits.OverflowBehavior.CUT) {
                columns = Limits.MAX_COLUMNS;
            } else {
                throw new LimitExceededException("columns", requestedColumns, Limits.MAX_COLUMNS);
            }
        }
        return new SheetLimits(columns, Limits.getRowOverflow());
    }

    public int columns() {
        return columns;
    }

    public int rows(int startRow, int requested) {
        if (startRow < 0 || requested < 0) {
            throw new IllegalArgumentException("startRow/requested must not be negative");
        }
        long end = (long) startRow + requested;
        if (end <= Limits.MAX_ROWS) {
            return requested;
        }
        if (rowBehavior == Limits.OverflowBehavior.CUT) {
            return Math.max(0, Limits.MAX_ROWS - startRow);
        }
        throw new LimitExceededException("rows", end, Limits.MAX_ROWS);
    }
}
