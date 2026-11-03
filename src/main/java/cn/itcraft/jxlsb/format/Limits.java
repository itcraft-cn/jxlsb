package cn.itcraft.jxlsb.format;

import cn.itcraft.jxlsb.api.LimitExceededException;

/**
 * Excel 行列上限与溢出策略。
 *
 * <p>硬上限为 {@link #MAX_COLUMNS} 列、{@link #MAX_ROWS} 行。超限行为由全局策略决定：
 * <ul>
 *   <li>{@link OverflowBehavior#ERROR}：抛出 {@link LimitExceededException}</li>
 *   <li>{@link OverflowBehavior#CUT}：静默截断到上限</li>
 * </ul>
 *
 * <p>配置是全局的，可随时修改；每次写入操作开始时通过 {@link SheetLimits} 快照，
 * 因此修改只影响其后的操作。列/行各自独立配置，{@link #setOverflow} 可一次设置两者。
 *
 * @author AI架构师
 * @since 1.2.0
 */
public final class Limits {

    /** Excel 规范最大列数（XFD）。 */
    public static final int MAX_COLUMNS = 16384;
    /** Excel 规范最大行数。 */
    public static final int MAX_ROWS = 1048576;

    /** 超出上限时的行为。 */
    public enum OverflowBehavior {
        ERROR,
        CUT
    }

    private static volatile OverflowBehavior columnBehavior = OverflowBehavior.ERROR;
    private static volatile OverflowBehavior rowBehavior = OverflowBehavior.ERROR;

    private Limits() {
    }

    public static void setColumnOverflow(OverflowBehavior behavior) {
        columnBehavior = behavior == OverflowBehavior.CUT ? OverflowBehavior.CUT : OverflowBehavior.ERROR;
    }

    public static void setRowOverflow(OverflowBehavior behavior) {
        rowBehavior = behavior == OverflowBehavior.CUT ? OverflowBehavior.CUT : OverflowBehavior.ERROR;
    }

    public static void setOverflow(OverflowBehavior columns, OverflowBehavior rows) {
        setColumnOverflow(columns);
        setRowOverflow(rows);
    }

    public static OverflowBehavior getColumnOverflow() {
        return columnBehavior;
    }

    public static OverflowBehavior getRowOverflow() {
        return rowBehavior;
    }
}
