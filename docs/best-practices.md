# jxlsb 最佳实践

本指南介绍生产环境中使用jxlsb的最佳实践、常见陷阱和优化建议。

## 目录

1. [资源管理最佳实践](#资源管理最佳实践)
2. [批量写入优化](#批量写入优化)
3. [大文件处理](#大文件处理)
4. [多线程处理](#多线程处理)
5. [内存池使用](#内存池使用)
6. [常见陷阱](#常见陷阱)
7. [生产环境建议](#生产环境建议)

---

## 资源管理最佳实践

### 必须使用try-with-resources

**正确示例：**

```java
// Writer自动关闭
try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("output.xlsb"))
        .build()) {
    
    writer.writeBatch("Sheet1", supplier, rows, cols);
    
} // 自动调用close()，释放堆外内存

// Reader自动关闭
try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("input.xlsb"))
        .build()) {
    
    reader.readSheets(sheet -> {
        // 处理Sheet
    });
    
} // 自动调用close()，释放堆外内存
```

**错误示例：**

```java
// 错误：未关闭Writer，导致堆外内存泄漏
XlsbWriter writer = XlsbWriter.builder()
    .path(Paths.get("output.xlsb"))
    .build();

writer.writeBatch(...);

// 忘记调用close()！堆外内存未释放
// writer.close();
```

### 异常处理要确保释放

```java
try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("output.xlsb"))
        .build()) {
    
    writer.writeBatch("Sheet1", supplier, rows, cols);
    
} catch (IOException e) {
    // try-with-resources已自动关闭writer
    log.error("写入失败", e);
    throw e;
}

// 即使发生异常，writer也会自动关闭
```

---

## 批量写入优化

### 使用writeBatch API

**最高效的方式：**

```java
writer.writeBatch("Sheet1", 
    (row, col) -> CellData.number(row * col),
    100000, 50);
```

**优势：**
- 内存池重用，减少分配开销
- 批量BIFF12记录写入
- 内部优化缓冲区管理

### 数据供应函数优化

**优化示例：**

```java
// 高效：简单计算
writer.writeBatch("Sheet1", 
    (row, col) -> CellData.number(row * col),
    100000, 50);

// 高效：switch分支
writer.writeBatch("Sheet1", 
    (row, col) -> {
        switch (col % 3) {
            case 0: return CellData.number(row);
            case 1: return CellData.text("T-" + row);
            case 2: return CellData.bool(row % 2 == 0);
            default: return CellData.blank();
        }
    },
    100000, 50);

// 避免复杂逻辑：影响性能
// 不推荐在supplier中进行复杂计算或数据库查询
```

### 多Sheet写入策略

```java
try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("multi.xlsb"))
        .build()) {
    
    // 先写小Sheet，后写大Sheet
    writer.writeBatch("摘要", supplier, 100, 10);   // 小Sheet
    writer.writeBatch("明细", supplier, 10000, 50); // 大Sheet
    
    // 顺序写入，避免内存占用过高
}
```

---

## 大文件处理

### 流式处理GB级文件

**写入大文件：**

```java
// 1M行 ≈ 30MB文件
try (XlsbWriter writer = XlsbWriter.builder()
        .path(Paths.get("large.xlsb"))
        .build()) {
    
    writer.writeBatch("大数据", 
        (row, col) -> CellData.number(row + col),
        1000000, 10);
}

// 10M行 ≈ 300MB文件（需要更多堆外内存）
// JVM配置：-XX:MaxDirectMemorySize=1g
```

**读取大文件：**

```java
try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("large.xlsb"))
        .build()) {
    
    reader.readSheets(sheet -> {
        // 流式读取，内存占用稳定（约1MB）
        for (OffHeapRow row : sheet) {
            // 逐行处理
            processRow(row);
        }
    });
}
```

### JVM内存配置建议

根据文件大小调整：

```bash
# 小文件（<10MB）
java -Xmx128m -XX:MaxDirectMemorySize=512m -jar app.jar

# 中文件（10-100MB）
java -Xmx256m -XX:MaxDirectMemorySize=1g -jar app.jar

# 大文件（100MB-1GB）
java -Xmx256m -XX:MaxDirectMemorySize=2g -jar app.jar

# 超大文件（>1GB）
java -Xmx512m -XX:MaxDirectMemorySize=4g -jar app.jar
```

---

## 多线程处理

### 线程安全性

**重要说明：**

- **XlsbWriter/XlsbReader不是线程安全的**
- **每个线程使用独立的Writer/Reader**
- **内存池是线程安全的，可以共享**

**错误示例：**

```java
// 错误：多个线程共享Writer
XlsbWriter writer = XlsbWriter.builder().path(...).build();

ExecutorService executor = Executors.newFixedThreadPool(4);
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        // 错误：并发写入同一个writer
        writer.writeBatch(...);  // 线程安全问题！
    });
}
```

**正确示例：**

```java
// 正确：每个线程独立Writer
ExecutorService executor = Executors.newFixedThreadPool(4);

for (int i = 0; i < 10; i++) {
    int fileIndex = i;
    executor.submit(() -> {
        Path path = Paths.get("output-" + fileIndex + ".xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(path)
                .build()) {
            
            writer.writeBatch("Sheet", supplier, 10000, 10);
        }
    });
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.MINUTES);
```

### 并发写入多个文件

```java
List<Path> outputFiles = IntStream.range(0, 100)
    .parallel()
    .map(i -> {
        Path path = Paths.get("output-" + i + ".xlsb");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(path)
                .build()) {
            
            writer.writeBatch("Data", 
                (row, col) -> CellData.number(row + col),
                10000, 10);
            
            return path;
        }
    })
    .collect(Collectors.toList());
```

---

## 内存池使用

### 内存池机制

jxlsb使用5级内存池：

```
SIZE_CLASSES = {
    64B    - 小单元格（单个数字）
    4KB    - 中单元格（短文本）
    64KB   - 大单元格（长文本）
    1MB    - 批量写入缓冲
    16MB   - 大块数据
};
```

### 内存池优势

- **重用内存块**：减少分配/释放开销
- **避免碎片**：预分配固定大小
- **减少系统调用**：内存块复用

### 内存池监控（可选）

如果需要监控内存池效果：

```java
// 获取内存池统计（假设API提供）
MemoryPool pool = AllocatorFactory.getMemoryPool();

System.out.println("命中率: " + pool.getHitRate());
System.out.println("分配次数: " + pool.getAllocationCount());
System.out.println("池大小: " + pool.getPoolSize());
```

---

## 常见陷阱

### 陷阱1：忘记关闭Writer/Reader

**问题：**
```java
XlsbWriter writer = XlsbWriter.builder().path(...).build();
writer.writeBatch(...);
// 未调用close() → 堆外内存泄漏
```

**后果：**
- 堆外内存不释放，导致内存占用持续增长
- 多次泄漏后可能耗尽堆外内存，抛出MemoryAllocationException

**解决：**
```java
try (XlsbWriter writer = XlsbWriter.builder().path(...).build()) {
    writer.writeBatch(...);
} // 自动关闭
```

### 陷阱2：超大堆外内存配置不足

**问题：**
```java
// 写入1M行，堆外内存配置太小
java -XX:MaxDirectMemorySize=128m -jar app.jar

// 抛出异常：
// MemoryAllocationException: Failed to allocate off-heap memory
```

**解决：**
```bash
# 根据文件大小配置足够的堆外内存
java -XX:MaxDirectMemorySize=2g -jar app.jar
```

### 陷阱3：多线程共享Writer/Reader

**问题：**
```java
// 多线程共享Writer
XlsbWriter sharedWriter = ...;

executor.submit(() -> sharedWriter.writeBatch(...)); // 线程安全问题
executor.submit(() -> sharedWriter.writeBatch(...)); // 数据混乱
```

**后果：**
- 数据混乱
- 写入失败
- 异常崩溃

**解决：**
```java
// 每个线程独立Writer
executor.submit(() -> {
    try (XlsbWriter writer = XlsbWriter.builder().path(...).build()) {
        writer.writeBatch(...);
    }
});
```

### 陷阱4：Supplier中复杂逻辑

**问题：**
```java
writer.writeBatch("Sheet", 
    (row, col) -> {
        // 每次调用查询数据库，性能极差
        return CellData.text(db.queryValue(row, col));
    },
    100000, 50);
```

**后果：**
- 性能急剧下降
- 数据库压力过大

**解决：**
```java
// 预加载数据
List<Data> dataCache = db.queryAllData();

writer.writeBatch("Sheet", 
    (row, col) -> {
        // 从缓存读取
        return CellData.text(dataCache.get(row).getValue(col));
    },
    100000, 50);
```

### 陷阱5：忽略异常处理

**问题：**
```java
XlsbWriter writer = XlsbWriter.builder().path(...).build();
try {
    writer.writeBatch(...);
} catch (IOException e) {
    // 异常后writer未关闭
    throw e;
}
```

**解决：**
```java
try (XlsbWriter writer = XlsbWriter.builder().path(...).build()) {
    writer.writeBatch(...);
} catch (IOException e) {
    // writer已自动关闭
    throw e;
}
```

---

## 生产环境建议

### 1. 完善的异常处理

```java
try (XlsbWriter writer = XlsbWriter.builder()
        .path(outputPath)
        .build()) {
    
    writer.writeBatch("Sheet", supplier, rows, cols);
    
} catch (XlsbException e) {
    log.error("XLSB格式错误: {}", e.getMessage());
    throw new BusinessException("文件生成失败", e);
    
} catch (MemoryAllocationException e) {
    log.error("内存不足: {}", e.getMessage());
    throw new BusinessException("内存配置不足，请联系管理员", e);
    
} catch (IOException e) {
    log.error("IO错误: {}", e.getMessage());
    throw new BusinessException("文件写入失败", e);
}
```

### 2. 监控和日志

```java
// 添加性能监控
long startTime = System.currentTimeMillis();

try (XlsbWriter writer = XlsbWriter.builder()
        .path(outputPath)
        .build()) {
    
    writer.writeBatch("Sheet", supplier, rows, cols);
    
    long duration = System.currentTimeMillis() - startTime;
    long fileSize = Files.size(outputPath);
    
    log.info("文件生成成功: {}, 大小: {}MB, 时间: {}ms, 行数: {}",
        outputPath, fileSize / 1024 / 1024, duration, rows);
}
```

### 3. 合理的JVM配置

**生产环境推荐配置：**

```bash
# 通用配置（支持中等规模文件）
java -Xmx256m \
     -XX:MaxDirectMemorySize=2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar your-app.jar

# 高性能配置（支持大规模文件）
java -Xmx512m \
     -XX:MaxDirectMemorySize=4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:G1NewSizePercent=30 \
     -jar your-app.jar
```

### 4. 资源清理机制

```java
// 定期清理临时文件
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
public void cleanupTempFiles() {
    Path tempDir = Paths.get("/tmp/xlsb");
    
    Files.walk(tempDir)
        .filter(p -> p.toString().endsWith(".xlsb"))
        .filter(p -> {
            try {
                return Files.getLastModifiedTime(p)
                    .toInstant()
                    .isBefore(Instant.now().minus(7, ChronoUnit.DAYS));
            } catch (IOException e) {
                return false;
            }
        })
        .forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", p);
            }
        });
}
```

### 5. 健康检查

```java
// 检查堆外内存可用性
@Component
public class MemoryHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // 尝试分配小块内存
            OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
            MemoryBlock block = allocator.allocate(1024);
            block.close();
            
            return Health.up()
                .withDetail("offHeapMemory", "OK")
                .build();
                
        } catch (MemoryAllocationException e) {
            return Health.down()
                .withDetail("offHeapMemory", "LOW")
                .withException(e)
                .build();
        }
    }
}
```

---

## 性能优化清单

在部署生产环境前，请检查以下项：

- ✅ 使用try-with-resources管理Writer/Reader
- ✅ 根据文件大小配置足够的堆外内存
- ✅ 使用批量写入API（writeBatch）
- ✅ 避免Supplier中的复杂逻辑
- ✅ 多线程使用独立Writer/Reader
- ✅ 完善的异常处理和日志
- ✅ 配置G1GC降低GC暂停
- ✅ 监控内存使用和GC频率
- ✅ 定期清理临时文件
- ✅ 性能测试验证满足需求

---

## 相关文档

- [API使用指南](api-guide.md) - API详细用法
- [性能指南](performance.md) - 性能优化方法
- [示例代码](examples/) - 最佳实践示例

---

**版本：** 1.0.0  
**更新日期：** 2026-04-10