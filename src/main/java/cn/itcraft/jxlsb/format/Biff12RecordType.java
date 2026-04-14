package cn.itcraft.jxlsb.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 正确的BIFF12记录类型常量
 * 根据MS-XLSB规范2.3.2节
 */
public final class Biff12RecordType {
    
    // Cell records (0-11)
    public static final int BrtRowHdr = 0;
    public static final int BrtCellBlank = 1;
    public static final int BrtCellRk = 2;
    public static final int BrtCellError = 3;
    public static final int BrtCellBool = 4;
    public static final int BrtCellReal = 5;
    public static final int BrtCellSt = 6;
    public static final int BrtCellIsst = 7;
    
    // SST
    public static final int BrtSSTItem = 19;
    
    // Workbook records (128+)
    public static final int BrtFileVersion = 128;
    public static final int BrtBeginSheet = 129;
    public static final int BrtEndSheet = 130;
    public static final int BrtBeginBook = 131;
    public static final int BrtEndBook = 132;
    public static final int BrtBeginWsViews = 133;
    public static final int BrtEndWsViews = 134;
    public static final int BrtBeginBookViews = 135;
    public static final int BrtEndBookViews = 136;
    public static final int BrtBeginWsView = 137;
    public static final int BrtEndWsView = 138;
    public static final int BrtBeginBundleShs = 143;
    public static final int BrtEndBundleShs = 144;
    public static final int BrtBeginSheetData = 145;
    public static final int BrtEndSheetData = 146;
    public static final int BrtWsProp = 147;
    public static final int BrtWsDim = 148;
    public static final int BrtPane = 151;
    public static final int BrtSel = 152;
    public static final int BrtWbProp = 153;
    public static final int BrtBundleSh = 156;
    public static final int BrtBookView = 158;
    public static final int BrtBeginSst = 159;
    public static final int BrtEndSst = 160;
    
    public static final int BrtPageSetup = 476;
    public static final int BrtPageSetupView = 477;
    public static final int BrtWsFmtInfo = 485;
    public static final int BrtDrawing = 535;
    
    public static final int BrtFmt = 44;
    public static final int BrtFont = 43;
    public static final int BrtFill = 45;
    public static final int BrtBorder = 46;
    public static final int BrtXF = 47;
    public static final int BrtBeginStyleSheet = 370;
    public static final int BrtEndStyleSheet = 371;
    public static final int BrtBeginCellStyleXFs = 278;
    public static final int BrtEndCellStyleXFs = 279;
    public static final int BrtBeginCellXFs = 280;
    public static final int BrtEndCellXFs = 281;
    
    private Biff12RecordType() {}
}