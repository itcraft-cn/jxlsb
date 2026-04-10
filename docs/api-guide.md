# jxlsb API使用指南

本指南详细介绍jxlsb库的API使用方法，包括完整可运行的代码示例。

## 目录

1. [写入API详解](#写入api详解)
2. [读取API详解](#读取api详解)
3. [单元格类型](#单元格类型)
4. [错误处理](#错误处理)
5. [资源管理](#资源管理)

---

## 写入API详解

### XlsbWriter基础用法

#### 创建Writer

使用Builder模式创建XlsbWriter：

```java
import cn.itcraft.jxlsb.api.XlsbWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class BasicWriteExample {
    public static void main(String[] args) throws IOException {
        // 指定输出文件路径
        Path outputPath = Paths.get("output.xlsb");
        
        // 使用Builder创建Writer（推荐try-with-resources）
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(outputPath)
                .build()) {
            
            // 写入数据
            writer.writeBatch("Sheet1", 
                (row, col) -> CellData.number(row * col),
                100, 10);
        }
        
        System.out.println("文件已创建: " + outputPath);
    }
}
```

#### 批量写入API

`writeBatch`是最高效的写入方式，适合大数据场景：

```java
import cn.itcraft.jxlsb.api.CellData;

try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("batch.xlsb"))
        .build()) {
    
    // 写入100K行数据
    writer.writeBatch("大数据表", 
        (row, col) -> {
            // 数据供应函数：根据row和col返回CellData
            switch (col % 4) {
                case 0: 
                    return CellData.text("产品-" + row);
                case 1: 
                    return CellData.number(row * 100.5);
                case 2: 
                    return CellData.date(System.currentTimeMillis());
                case 3: 
                    return CellData.bool(row % 2 == 0);
                default: 
                    return CellData.blank();
            }
        },
        100000,  // 10万行
        50       // 50列
    );
}
```

**参数说明：**
- `sheetName`: Sheet名称
- `supplier`: `CellDataSupplier`函数式接口，接收(row, col)，返回CellData
- `rowCount`: 行数
- `columnCount`: 列数

#### 多Sheet写入

支持在一个Workbook中写入多个Sheet：

```java
try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("multi-sheet.xlsb"))
        .build()) {
    
    // Sheet 1: 数字数据
    writer.writeBatch("销售数据", 
        (row, col) -> CellData.number(Math.random() * 1000),
        1000, 20);
    
    // Sheet 2: 文本数据
    writer.writeBatch("产品列表", 
        (row, col) -> CellData.text("产品-" + row + "-" + col),
        500, 10);
    
    // Sheet 3: 混合数据
    writer.writeBatch("综合报表", 
        (row, col) -> {
            if (col == 0) return CellData.text("行" + row);
            return CellData.number(row * col);
        },
        2000, 15);
}
```

---

## 读取API详解

### XlsbReader基础用法

#### 创建Reader

```java
import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import java.nio.file.Paths;

public class BasicReadExample {
    public static void main(String[] args) throws IOException {
        Path inputFile = Paths.get("data.xlsb");
        
        try (XlsbReader reader = XlsbReader.builder()
                .path(inputFile)
                .build()) {
            
            // 流式读取所有Sheet
            reader.readSheets(sheet -> {
                System.out.println("Sheet名称: " + sheet.getSheetName());
                System.out.println("Sheet索引: " + sheet.getSheetIndex());
                
                // 遍历行
                for (OffHeapRow row : sheet) {
                    System.out.println("行号: " + row.getRowIndex());
                    
                    // 遍历单元格
                    for (int i = 0; i < row.getColumnCount(); i++) {
                        OffHeapCell cell = row.getCell(i);
                        System.out.println("  列" + i + ": " + formatCell(cell));
                    }
                }
            });
        }
    }
    
    private static String formatCell(OffHeapCell cell) {
        switch (cell.getType()) {
            case TEXT: return cell.getText();
            case NUMBER: return String.valueOf(cell.getNumber());
            case DATE: return String.valueOf(cell.getDate());
            case BOOLEAN: return String.valueOf(cell.getBoolean());
            case BLANK: return "";
            default: return "?";
        }
    }
}
```

#### 流式读取API

使用`readSheets`处理大规模数据，避免全量加载：

```java
try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("large.xlsb"))
        .build()) {
    
    // 流式处理，内存占用稳定
    reader.readSheets(sheet -> {
        // Sheet处理回调
        System.out.println("处理Sheet: " + sheet.getSheetName());
        
        // 使用迭代器逐行处理
        for (OffHeapRow row : sheet) {
            // 处理每行数据
            processRow(row);
        }
        
        // Sheet处理完成后自动释放内存
    });
}
```

#### 指定Sheet读取

读取特定Sheet而非全部：

```java
try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("multi.xlsb"))
        .build()) {
    
    // 只读取第一个Sheet
    OffHeapSheet sheet0 = reader.readSheet(0);
    
    // 处理Sheet数据
    for (OffHeapRow row : sheet0) {
        // 处理逻辑
    }
    
    // 手动关闭Sheet（如果不再使用）
    sheet0.close();
    
    // 读取第二个Sheet
    OffHeapSheet sheet1 = reader.readSheet(1);
    // ...
}
```

---

## 单元格类型

### CellType枚举

```java
public enum CellType {
    TEXT,     // 文本字符串
    NUMBER,   // IEEE 754双精度浮点数
    DATE,     // Unix毫秒时间戳
    BOOLEAN,  // 布尔值
    BLANK     // 空白单元格
}
```

### CellData创建方法

```java
// 文本
CellData text = CellData.text("Hello World");
CellData text2 = CellData.text("中文文本支持");

// 数字（支持整数、小数）
CellData number = CellData.number(42);
CellData decimal = CellData.number(3.141592653589793);
CellData negative = CellData.number(-1234.56);

// 日期（Unix毫秒时间戳）
CellData date1 = CellData.date(System.currentTimeMillis());
CellData date2 = CellData.date(1609459200000L); // 2021-01-01 00:00:00 UTC

// 布尔
CellData bool1 = CellData.bool(true);
CellData bool2 = CellData.bool(false);

// 空白（空单元格）
CellData blank = CellData.blank();
```

### OffHeapCell读取方法

```java
OffHeapCell cell = row.getCell(col);

// 获取类型
CellType type = cell.getType();

// 根据类型获取值
switch (type) {
    case TEXT:
        String text = cell.getText();
        break;
        
    case NUMBER:
        double number = cell.getNumber();
        break;
        
    case DATE:
        long timestamp = cell.getDate(); // Unix毫秒时间戳
        // 转换为Java Date
        Date date = new Date(timestamp);
        break;
        
    case BOOLEAN:
        boolean bool = cell.getBoolean();
        break;
        
    case BLANK:
        // 空单元格，无值
        break;
}
```

---

## 错误处理

### 异常类型

```java
// 基础异常
cn.itcraft.jxlsb.exception.XlsbException

// 内存分配异常
cn.itcraft.jxlsb.exception.MemoryAllocationException
```

### 异常捕获示例

```java
import cn.itcraft.jxlsb.exception.XlsbException;
import cn.itcraft.jxlsb.exception.MemoryAllocationException;

try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("output.xlsb"))
        .build()) {
    
    writer.writeBatch("Sheet1", 
        (row, col) -> CellData.number(row * col),
        100000, 100);
        
} catch (XlsbException e) {
    // XLSB格式相关错误
    System.err.println("XLSB格式错误: " + e.getMessage());
    
} catch (MemoryAllocationException e) {
    // 内存分配失败
    System.err.println("内存不足: " + e.getMessage());
    System.err.println("建议: 增加堆外内存配置 -XX:MaxDirectMemorySize");
    
} catch (IOException e) {
    // IO错误
    System.err.println("文件IO错误: " + e.getMessage());
}
```

### 资源管理最佳实践

**推荐：使用try-with-resources**

```java
// Writer自动关闭
try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("output.xlsb"))
        .build()) {
    writer.writeBatch(...);
} // 自动调用close()，释放资源

// Reader自动关闭
try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("input.xlsb"))
        .build()) {
    reader.readSheets(...);
} // 自动调用close()，释放堆外内存
```

**不推荐：手动管理（容易忘记关闭）**

```java
XlsbWriter writer = XlsbWriter.builder()
    .path(Paths.get("output.xlsb"))
    .build();

try {
    writer.writeBatch(...);
} finally {
    // 必须手动关闭！
    writer.close();
}
```

---

## 资源管理

### 内存生命周期

```
┌─────────────┐
│ 创建Writer   │  → 分配堆外内存
│ 或Reader    │
├─────────────┤
│ 写入/读取    │  → 使用内存池，重用内存块
│ 数据        │
├─────────────┤
│ close()     │  → 释放堆外内存，归还内存池
└─────────────┘
```

### 重要提醒

1. **必须调用close()**：否则堆外内存不会释放，可能导致内存泄漏
2. **推荐try-with-resources**：自动管理资源，避免忘记关闭
3. **异常处理要包含close()**：即使发生异常也要释放资源

---

## 完整示例代码

### 示例1：简单写入

```java
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import java.nio.file.Paths;
import java.io.IOException;

public class SimpleWrite {
    public static void main(String[] args) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(Paths.get("simple.xlsb"))
                .build()) {
            
            // 写入10行5列
            writer.writeBatch("数据", 
                (row, col) -> {
                    if (col == 0) return CellData.text("行" + row);
                    return CellData.number(row * 100 + col);
                },
                10, 5);
        }
        
        System.out.println("完成: simple.xlsb");
    }
}
```

### 示例2：大数据写入

```java
import cn.itcraft.jxlsb.api.XlsbWriter;
import cn.itcraft.jxlsb.api.CellData;
import java.nio.file.Paths;

public class LargeDataWrite {
    public static void main(String[] args) throws IOException {
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(Paths.get("large.xlsb"))
                .build()) {
            
            // 写入100万行数据（约30MB文件）
            writer.writeBatch("百万数据", 
                (row, col) -> CellData.number(row + col * 0.001),
                1000000, 10);
        }
        
        System.out.println("完成: large.xlsb (100万行)");
    }
}
```

### 示例3：流式读取

```java
import cn.itcraft.jxlsb.api.XlsbReader;
import cn.itcraft.jxlsb.data.OffHeapSheet;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import java.nio.file.Paths;

public class StreamRead {
    public static void main(String[] args) throws IOException {
        try (XlsbReader reader = XlsbReader.builder()
                .path(Paths.get("large.xlsb"))
                .build()) {
            
            int totalRows = 0;
            int totalCells = 0;
            
            reader.readSheets(sheet -> {
                for (OffHeapRow row : sheet) {
                    totalRows++;
                    totalCells += row.getColumnCount();
                }
            });
            
            System.out.println("总行数: " + totalRows);
            System.out.println("总单元格: " + totalCells);
        }
    }
}
```

---

## 相关文档

- [性能指南](performance.md) - 性能优化建议
- [最佳实践](best-practices.md) - 生产环境使用建议
- [示例代码](examples/) - 完整可运行示例

---

**版本：** 1.0.0  
**更新日期：** 2026-04-10