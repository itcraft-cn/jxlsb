package cn.itcraft.jxlsb.format;

public final class Biff12Constants {
    
    // File structure records
    public static final int FILE_VERSION = 0x0080;
    public static final int END_FILE_VERSION = 0x0081;
    
    // Workbook records
    public static final int BEGIN_BOOK = 0x0083;
    public static final int END_BOOK = 0x0084;
    public static final int BEGIN_BUNDLE_SHS = 0x0085;
    public static final int END_BUNDLE_SHS = 0x0086;
    public static final int BEGIN_SHEET_DATA = 0x0091;
    public static final int END_SHEET_DATA = 0x0092;
    
    // Worksheet records
    public static final int WS_DIMENSION = 0x0094;
    public static final int BEGIN_SHEET = 0x0089;
    public static final int END_SHEET = 0x008A;
    
    // Row/Cell records
    public static final int ROW = 0x0000;
    public static final int CELL_BLANK = 0x0001;
    public static final int CELL_BOOL = 0x0002;
    public static final int CELL_ERROR = 0x0003;
    public static final int CELL_FLOAT = 0x0004;
    public static final int CELL_STRING = 0x0005;
    public static final int CELL_RICH_STRING = 0x0006;
    
    // Shared strings
    public static final int BEGIN_SST = 0x009F;
    public static final int END_SST = 0x00A0;
    public static final int SST_ITEM = 0x0013;
    
    // Sheets
    public static final int SHEET = 0x009C;
    
    private Biff12Constants() {}
}