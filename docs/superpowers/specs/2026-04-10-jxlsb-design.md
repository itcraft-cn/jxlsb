# jxlsb 设计文档

## 一、项目概述

### 1.1 项目目标

构建一个纯Java实现的XLSB（Excel Binary Workbook）格式读写库，具备以下特性：

- **零第三方依赖**：仅依赖Java SDK和SLF4J
- **全量堆外内存**：所有数据结构和缓冲区均使用堆外内存，追求极致性能和零GC压力
- **多JDK版本支持**：Java 8基础实现，Java 17+高级实现（通过src/main/java17多态支持）
- **企业级质量**：完整测试覆盖、流式API、异常处理、资源管理
- **流式处理**：支持GB级大文件流式读写

### 1.2 功能范围

**基础读写功能：**
- 单元格数据类型：文本、数值、日期、布尔、错误值
- 行/列操作：读取、写入、批量操作
- 多Sheet支持：Workbook包含多个Sheet
- 基础样式：字体、颜色、边框（仅读取）

**流式API：**
- Builder模式构建读写器
- 函数式接口处理数据
- 流式管道避免全量加载

**性能优化：**
- 内存池管理堆外内存块
- 零拷贝FileChannel读写
- 批量读写优化

### 1.3 技术约束

- 包名：`cn.itcraft.jxlsb`
- Group ID：`cn.itcraft`
- Artifact ID：`jxlsb`
- 最低JDK：Java 8（推荐Java 17）
- 构建：Maven（mvnd优先）
- 日志：SLF4J（仅API，不绑定实现）
- 测试：JUnit 5 + Mockito + 性能测试 + 内存监控

---

## 二、整体架构

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                      API Layer (流式API)                     │
│  XlsbReader / XlsbWriter / SheetReader / SheetWriter        │
│  + Builder模式 + 函数式接口                                   │
├─────────────────────────────────────────────────────────────┤
│                    Data Structure Layer                      │
│  OffHeapCell / OffHeapRow / OffHeapSheet / OffHeapWorkbook  │
│  所有数据对象均基于堆外内存块                                   │
├─────────────────────────────────────────────────────────────┤
│                    Memory Management Layer                   │
│  ┌──────────────────────┬─────────────────────────────┐     │
│  │   Java 8 Impl        │      Java 17 Impl          │     │
│  │  ByteBufferStrategy  │   MemorySegmentStrategy    │     │
│  │  UnsafeByteOps       │   MemoryAccess API         │     │
│  └──────────────────────┴─────────────────────────────┘     │
│  OffHeapAllocator / MemoryBlock / MemoryPool                │
├─────────────────────────────────────────────────────────────┤
│                   XLSB Binary Format Layer                   │
│  RecordParser / RecordWriter / BIFF12 Records               │
│  基于[MS-XLSB]协议规范解析                                    │
├─────────────────────────────────────────────────────────────┤
│                      IO Layer                                │
│  OffHeapInputStream / OffHeapOutputStream                   │
│  直接FileChannel读写堆外内存，零拷贝                          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心设计原则

1. **堆外内存优先**：所有数据在堆外内存中操作，堆上仅有轻量级引用对象
2. **零拷贝IO**：FileChannel直接读写堆外内存块
3. **内存池化**：避免频繁分配释放堆外内存，复用内存块
4. **流式处理**：避免全量加载到内存，支持GB级文件
5. **资源安全**：所有堆外内存资源通过try-with-resources或 Cleaner释放
6. **多JDK适配**：通过ServiceLoader机制自动加载最优实现

---

## 三、内存管理层详细设计

### 3.1 内存管理抽象接口

```java
package cn.itcraft.jxlsb.memory;

/**
 * 堆外内存分配器接口
 * 
 * <p>定义堆外内存分配、释放、访问的抽象操作。
 * Java 8和Java 17有不同实现策略。
 */
public interface OffHeapAllocator {
    
    /**
     * 分配指定大小的堆外内存块
     */
    MemoryBlock allocate(long size);
    
    /**
     * 从内存池获取可复用内存块
     */
    MemoryBlock allocateFromPool(long size);
    
    /**
     * 获取当前内存分配器策略名称
     */
    String getStrategyName();
    
    /**
     * 获取当前已分配的总堆外内存大小
     */
    long getTotalAllocated();
}
```

```java
package cn.itcraft.jxlsb.memory;

/**
 * 堆外内存块抽象
 * 
 * <p>代表一块堆外内存，提供读写操作。
 * 内存块由MemoryPool管理，支持复用。
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
     * 写入字节数组
     */
    void putBytes(long offset, byte[] src, int srcOffset, int length);
    
    /**
     * 读取字节
     */
    byte getByte(long offset);
    
    /**
     * 读取short
     */
    short getShort(long offset);
    
    /**
     * 读取int
     */
    int getInt(long offset);
    
    /**
     * 读取long
     */
    long getLong(long offset);
    
    /**
     * 读取字节数组
     */
    void getBytes(long offset, byte[] dst, int dstOffset, int length);
    
    /**
     * 获取内存块大小
     */
    long size();
    
    /**
     * 获取内存块基地址（用于FileChannel零拷贝）
     */
    long getAddress();
    
    /**
     * 释放内存块（归还到内存池或直接释放）
     */
    @Override
    void close();
}
```

### 3.2 Java 8实现：ByteBuffer + Unsafe

**目录：** `src/main/java/cn/itcraft/jxlsb/memory/`

```java
package cn.itcraft.jxlsb.memory.impl;

/**
 * Java 8堆外内存分配器实现
 * 
 * <p>使用ByteBuffer.allocateDirect()分配堆外内存，
 * 通过Unsafe进行高效的内存访问操作。
 */
public final class ByteBufferAllocator implements OffHeapAllocator {
    
    private static final sun.misc.Unsafe UNSAFE;
    private static final long BYTE_BUFFER_ADDRESS_OFFSET;
    
    private final String strategyName = "ByteBuffer-Unsafe";
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final MemoryPool memoryPool;
    
    static {
        UNSAFE = getUnsafe();
        BYTE_BUFFER_ADDRESS_OFFSET = findByteBufferAddressOffset();
    }
    
    @Override
    public MemoryBlock allocate(long size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);
        long address = getAddress(buffer);
        totalAllocated.addAndGet(size);
        return new ByteBufferMemoryBlock(buffer, address, size, this);
    }
    
    @Override
    public MemoryBlock allocateFromPool(long size) {
        return memoryPool.acquire(size);
    }
    
    private static sun.misc.Unsafe getUnsafe() {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot access Unsafe", e);
        }
    }
    
    private long getAddress(ByteBuffer buffer) {
        return UNSAFE.getLong(buffer, BYTE_BUFFER_ADDRESS_OFFSET);
    }
}
```

```java
package cn.itcraft.jxlsb.memory.impl;

/**
 * Java 8内存块实现
 * 
 * <p>基于ByteBuffer的堆外内存块，通过Unsafe直接访问内存地址。
 */
final class ByteBufferMemoryBlock implements MemoryBlock {
    
    private final ByteBuffer buffer;
    private final long address;
    private final long size;
    private final ByteBufferAllocator allocator;
    private volatile boolean closed = false;
    
    ByteBufferMemoryBlock(ByteBuffer buffer, long address, long size, 
                         ByteBufferAllocator allocator) {
        this.buffer = buffer;
        this.address = address;
        this.size = size;
        this.allocator = allocator;
    }
    
    @Override
    public void putByte(long offset, byte value) {
        checkBounds(offset, 1);
        UNSAFE.putByte(address + offset, value);
    }
    
    @Override
    public void putInt(long offset, int value) {
        checkBounds(offset, 4);
        UNSAFE.putInt(address + offset, value);
    }
    
    @Override
    public byte getByte(long offset) {
        checkBounds(offset, 1);
        return UNSAFE.getByte(address + offset);
    }
    
    @Override
    public long getAddress() {
        return address;
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            allocator.memoryPool.release(this);
        }
    }
    
    private void checkBounds(long offset, int size) {
        if (offset < 0 || offset + size > this.size) {
            throw new IndexOutOfBoundsException(
                "Offset " + offset + " out of bounds for block size " + this.size);
        }
    }
}
```

### 3.3 Java 17实现：MemorySegment

**目录：** `src/main/java17/cn/itcraft/jxlsb/memory/impl/`

```java
package cn.itcraft.jxlsb.memory.impl;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.Addressable;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.FunctionDescriptor;

/**
 * Java 17堆外内存分配器实现
 * 
 * <p>使用Foreign Memory API（MemorySegment）管理堆外内存，
 * 更安全、更现代化，性能与Unsafe相当。
 */
public final class MemorySegmentAllocator implements OffHeapAllocator {
    
    private final String strategyName = "MemorySegment-ForeignAPI";
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final MemoryPool memoryPool;
    private final MemorySession globalSession;
    
    public MemorySegmentAllocator() {
        this.globalSession = MemorySession.global();
        this.memoryPool = new MemoryPool(this);
    }
    
    @Override
    public MemoryBlock allocate(long size) {
        MemorySegment segment = MemorySegment.allocateNative(size, globalSession);
        totalAllocated.addAndGet(size);
        return new MemorySegmentBlock(segment, size, this);
    }
    
    @Override
    public MemoryBlock allocateFromPool(long size) {
        return memoryPool.acquire(size);
    }
    
    @Override
    public String getStrategyName() {
        return strategyName;
    }
}
```

```java
package cn.itcraft.jxlsb.memory.impl;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Java 17内存块实现
 * 
 * <p>基于MemorySegment的堆外内存块，使用Foreign Memory API访问。
 */
final class MemorySegmentBlock implements MemoryBlock {
    
    private final MemorySegment segment;
    private final long size;
    private final MemorySegmentAllocator allocator;
    private volatile boolean closed = false;
    
    MemorySegmentBlock(MemorySegment segment, long size, 
                      MemorySegmentAllocator allocator) {
        this.segment = segment;
        this.size = size;
        this.allocator = allocator;
    }
    
    @Override
    public void putByte(long offset, byte value) {
        checkBounds(offset, 1);
        segment.set(ValueLayout.JAVA_BYTE, offset, value);
    }
    
    @Override
    public void putInt(long offset, int value) {
        checkBounds(offset, 4);
        segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, value);
    }
    
    @Override
    public byte getByte(long offset) {
        checkBounds(offset, 1);
        return segment.get(ValueLayout.JAVA_BYTE, offset);
    }
    
    @Override
    public long getAddress() {
        return segment.address().toRawLongValue();
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            allocator.memoryPool.release(this);
        }
    }
}
```

### 3.4 内存池管理

```java
package cn.itcraft.jxlsb.memory;

/**
 * 堆外内存池
 * 
 * <p>管理可复用的堆外内存块，避免频繁分配释放。
 * 使用分段池策略，不同大小的内存块分类管理。
 */
public final class MemoryPool implements AutoCloseable {
    
    private final OffHeapAllocator allocator;
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<MemoryBlock>> pool;
    private final AtomicLong totalPooledSize = new AtomicLong(0);
    
    private static final long[] SIZE_CLASSES = {
        64L,      // 64B
        4 * 1024L, // 4KB
        64 * 1024L, // 64KB
        1024 * 1024L, // 1MB
        16 * 1024 * 1024L // 16MB
    };
    
    public MemoryPool(OffHeapAllocator allocator) {
        this.allocator = allocator;
        this.pool = new ConcurrentHashMap<>();
    }
    
    /**
     * 从池中获取内存块，若不存在则新建
     */
    public MemoryBlock acquire(long size) {
        long classSize = findSizeClass(size);
        Queue<MemoryBlock> queue = pool.get(classSize);
        if (queue != null) {
            MemoryBlock block = queue.poll();
            if (block != null) {
                return block;
            }
        }
        return allocator.allocate(classSize);
    }
    
    /**
     * 归还内存块到池
     */
    public void release(MemoryBlock block) {
        long classSize = findSizeClass(block.size());
        pool.computeIfAbsent(classSize, k -> new ConcurrentLinkedQueue<>())
            .offer(block);
    }
    
    /**
     * 清空内存池，释放所有内存块
     */
    @Override
    public void close() {
        pool.forEach((size, queue) -> {
            queue.forEach(block -> {
                try {
                    allocator.deallocate(block);
                } catch (Exception e) {
                    log.warn("Failed to deallocate memory block", e);
                }
            });
        });
        pool.clear();
    }
    
    private long findSizeClass(long size) {
        for (long classSize : SIZE_CLASSES) {
            if (size <= classSize) {
                return classSize;
            }
        }
        return size;
    }
}
```

---

## 四、数据结构层详细设计

### 4.1 堆外单元格（OffHeapCell）

```java
package cn.itcraft.jxlsb.data;

/**
 * 堆外单元格数据结构
 * 
 * <p>单元格数据存储在堆外内存块中，堆上仅持有引用。
 * 支持类型：文本、数值、日期、布尔、错误。
 */
public final class OffHeapCell implements AutoCloseable {
    
    private static final int TYPE_OFFSET = 0;
    private static final int VALUE_OFFSET = 4;
    private static final int MAX_TEXT_SIZE = 1024;
    
    private final MemoryBlock memoryBlock;
    private final int rowIndex;
    private final int colIndex;
    
    public enum CellType {
        TEXT(0),
        NUMBER(1),
        DATE(2),
        BOOLEAN(3),
        ERROR(4),
        BLANK(5);
        
        private final int code;
        CellType(int code) { this.code = code; }
        public int getCode() { return code; }
    }
    
    public OffHeapCell(MemoryBlock memoryBlock, int rowIndex, int colIndex) {
        this.memoryBlock = memoryBlock;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
    }
    
    public void setText(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        memoryBlock.putInt(TYPE_OFFSET, CellType.TEXT.getCode());
        memoryBlock.putInt(VALUE_OFFSET, bytes.length);
        memoryBlock.putBytes(VALUE_OFFSET + 4, bytes, 0, bytes.length);
    }
    
    public void setNumber(double value) {
        memoryBlock.putInt(TYPE_OFFSET, CellType.NUMBER.getCode());
        memoryBlock.putDouble(VALUE_OFFSET, value);
    }
    
    public void setDate(long timestamp) {
        memoryBlock.putInt(TYPE_OFFSET, CellType.DATE.getCode());
        memoryBlock.putLong(VALUE_OFFSET, timestamp);
    }
    
    public CellType getType() {
        int code = memoryBlock.getInt(TYPE_OFFSET);
        return CellType.values()[code];
    }
    
    public String getText() {
        if (getType() != CellType.TEXT) {
            throw new IllegalStateException("Cell is not text type");
        }
        int length = memoryBlock.getInt(VALUE_OFFSET);
        byte[] bytes = new byte[length];
        memoryBlock.getBytes(VALUE_OFFSET + 4, bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public double getNumber() {
        if (getType() != CellType.NUMBER) {
            throw new IllegalStateException("Cell is not number type");
        }
        return memoryBlock.getDouble(VALUE_OFFSET);
    }
    
    @Override
    public void close() {
        memoryBlock.close();
    }
}
```

### 4.2 堆外行（OffHeapRow）

```java
package cn.itcraft.jxlsb.data;

/**
 * 堆外行数据结构
 * 
 * <p>一行数据由多个单元格组成，行本身持有堆外内存块数组引用。
 */
public final class OffHeapRow implements AutoCloseable {
    
    private final MemoryBlock[] cellBlocks;
    private final int rowIndex;
    private final int columnCount;
    private final OffHeapAllocator allocator;
    
    private static final long CELL_BLOCK_SIZE = 1024L;
    
    public OffHeapRow(int rowIndex, int columnCount, OffHeapAllocator allocator) {
        this.rowIndex = rowIndex;
        this.columnCount = columnCount;
        this.allocator = allocator;
        this.cellBlocks = new MemoryBlock[columnCount];
        
        for (int i = 0; i < columnCount; i++) {
            cellBlocks[i] = allocator.allocateFromPool(CELL_BLOCK_SIZE);
        }
    }
    
    public OffHeapCell getCell(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new IndexOutOfBoundsException("Column index out of bounds");
        }
        return new OffHeapCell(cellBlocks[columnIndex], rowIndex, columnIndex);
    }
    
    public void setCell(int columnIndex, OffHeapCell cell) {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new IndexOutOfBoundsException("Column index out of bounds");
        }
        cellBlocks[columnIndex] = cell.memoryBlock;
    }
    
    public int getRowIndex() {
        return rowIndex;
    }
    
    public int getColumnCount() {
        return columnCount;
    }
    
    @Override
    public void close() {
        for (MemoryBlock block : cellBlocks) {
            if (block != null) {
                block.close();
            }
        }
    }
}
```

### 4.3 堆外Sheet（OffHeapSheet）

```java
package cn.itcraft.jxlsb.data;

/**
 * 堆外Sheet数据结构
 * 
 * <p>Sheet管理所有行数据，支持流式读取（避免一次性加载所有行）。
 */
public final class OffHeapSheet implements AutoCloseable, Iterable<OffHeapRow> {
    
    private final String sheetName;
    private final int sheetIndex;
    private final int rowCount;
    private final int columnCount;
    private final OffHeapAllocator allocator;
    
    private OffHeapRow currentRow;
    private int currentRowIndex = -1;
    
    public OffHeapSheet(String sheetName, int sheetIndex, 
                       int rowCount, int columnCount,
                       OffHeapAllocator allocator) {
        this.sheetName = sheetName;
        this.sheetIndex = sheetIndex;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.allocator = allocator;
    }
    
    /**
     * 流式读取：创建下一行
     */
    public OffHeapRow nextRow() {
        if (currentRowIndex >= rowCount - 1) {
            return null;
        }
        
        if (currentRow != null) {
            currentRow.close();
        }
        
        currentRowIndex++;
        currentRow = new OffHeapRow(currentRowIndex, columnCount, allocator);
        return currentRow;
    }
    
    /**
     * 获取当前行
     */
    public OffHeapRow currentRow() {
        return currentRow;
    }
    
    /**
     * 创建指定索引的行（用于写入）
     */
    public OffHeapRow createRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("Row index out of bounds");
        }
        return new OffHeapRow(rowIndex, columnCount, allocator);
    }
    
    @Override
    public Iterator<OffHeapRow> iterator() {
        return new SheetRowIterator(this);
    }
    
    @Override
    public void close() {
        if (currentRow != null) {
            currentRow.close();
        }
    }
    
    private static final class SheetRowIterator implements Iterator<OffHeapRow> {
        private final OffHeapSheet sheet;
        
        SheetRowIterator(OffHeapSheet sheet) {
            this.sheet = sheet;
        }
        
        @Override
        public boolean hasNext() {
            return sheet.currentRowIndex < sheet.rowCount - 1;
        }
        
        @Override
        public OffHeapRow next() {
            return sheet.nextRow();
        }
    }
}
```

---

## 五、XLSB二进制格式层详细设计

### 5.1 XLSB格式概述（基于[MS-XLSB]规范）

XLSB是Excel二进制工作簿格式，使用BIFF12（Binary Interchange File Format 12）记录结构。

**关键记录类型：**

| Record Type | 代码 | 描述 |
|-------------|------|------|
| BR_BEGINBOOK | 0x0808 | Workbook开始 |
| BR_ENDBOOK | 0x0809 | Workbook结束 |
| BR_BEGINBUNDLESHS | 0x0841 | Sheet集合开始 |
| BR_BEGINSHEET | 0x0085 | Sheet开始 |
| BR_ENDSHEET | 0x0086 | Sheet结束 |
| BR_BEGINROW | 0x0087 | Row开始 |
| BR_CELL | 0x0143 | Cell数据 |
| BR_STRING | 0x00F9 | 字符串 |

### 5.2 RecordParser设计

```java
package cn.itcraft.jxlsb.format;

/**
 * XLSB记录解析器
 * 
 * <p>解析BIFF12二进制记录，将堆外内存块数据转换为Java对象。
 */
public final class RecordParser {
    
    private static final int RECORD_HEADER_SIZE = 4;
    
    private final OffHeapAllocator allocator;
    
    public RecordParser(OffHeapAllocator allocator) {
        this.allocator = allocator;
    }
    
    /**
     * 从堆外内存块解析记录
     */
    public BiffRecord parse(MemoryBlock block, long offset) {
        int recordType = block.getInt(offset);
        int recordSize = block.getInt(offset + 4);
        
        MemoryBlock recordData = allocator.allocateFromPool(recordSize);
        block.getBytes(offset + RECORD_HEADER_SIZE, 
                      recordData, 0, recordSize);
        
        return BiffRecord.create(recordType, recordSize, recordData);
    }
    
    /**
     * 批量解析记录（用于大文件流式处理）
     */
    public void parseStream(MemoryBlock block, 
                           RecordHandler handler) {
        long offset = 0;
        long blockSize = block.size();
        
        while (offset < blockSize - RECORD_HEADER_SIZE) {
            BiffRecord record = parse(block, offset);
            handler.handle(record);
            offset += RECORD_HEADER_SIZE + record.getSize();
            record.close();
        }
    }
}
```

```java
package cn.itcraft.jxlsb.format;

/**
 * BIFF12记录抽象
 * 
 * <p>所有记录类型继承此基类，数据存储在堆外内存块中。
 */
public abstract class BiffRecord implements AutoCloseable {
    
    protected final MemoryBlock dataBlock;
    protected final int recordType;
    protected final int recordSize;
    
    protected BiffRecord(int recordType, int recordSize, MemoryBlock dataBlock) {
        this.recordType = recordType;
        this.recordSize = recordSize;
        this.dataBlock = dataBlock;
    }
    
    public static BiffRecord create(int type, int size, MemoryBlock block) {
        switch (type) {
            case 0x0085: return new BeginSheetRecord(block, size);
            case 0x0087: return new BeginRowRecord(block, size);
            case 0x0143: return new CellRecord(block, size);
            default: return new UnknownRecord(type, size, block);
        }
    }
    
    public abstract void writeTo(RecordWriter writer);
    
    @Override
    public void close() {
        if (dataBlock != null) {
            dataBlock.close();
        }
    }
}
```

### 5.3 具体记录类型实现

```java
package cn.itcraft.jxlsb.format.record;

/**
 * Cell记录实现
 */
public final class CellRecord extends BiffRecord {
    
    private static final int ROW_OFFSET = 0;
    private static final int COL_OFFSET = 4;
    private static final int TYPE_OFFSET = 8;
    private static final int VALUE_OFFSET = 12;
    
    public CellRecord(MemoryBlock block, int size) {
        super(0x0143, size, block);
    }
    
    public int getRowIndex() {
        return dataBlock.getInt(ROW_OFFSET);
    }
    
    public int getColIndex() {
        return dataBlock.getInt(COL_OFFSET);
    }
    
    public CellType getCellType() {
        int typeCode = dataBlock.getInt(TYPE_OFFSET);
        return CellType.fromCode(typeCode);
    }
    
    public double getNumberValue() {
        return dataBlock.getDouble(VALUE_OFFSET);
    }
    
    @Override
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(0x0143, recordSize);
        writer.writeMemoryBlock(dataBlock);
    }
}
```

```java
package cn.itcraft.jxlsb.format.record;

/**
 * BeginSheet记录实现
 */
public final class BeginSheetRecord extends BiffRecord {
    
    private static final int SHEET_INDEX_OFFSET = 0;
    
    public BeginSheetRecord(MemoryBlock block, int size) {
        super(0x0085, size, block);
    }
    
    public int getSheetIndex() {
        return dataBlock.getInt(SHEET_INDEX_OFFSET);
    }
    
    @Override
    public void writeTo(RecordWriter writer) {
        writer.writeRecordHeader(0x0085, recordSize);
        writer.writeInt(getSheetIndex());
    }
}
```

---

## 六、IO层详细设计

### 6.1 堆外输入流

```java
package cn.itcraft.jxlsb.io;

/**
 * 堆外内存输入流
 * 
 * <p>直接从FileChannel读取数据到堆外内存块，零拷贝。
 */
public final class OffHeapInputStream implements AutoCloseable {
    
    private final FileChannel fileChannel;
    private final OffHeapAllocator allocator;
    private final long fileSize;
    private long position = 0;
    
    private static final long DEFAULT_BLOCK_SIZE = 16 * 1024 * 1024; // 16MB
    
    public OffHeapInputStream(Path path, OffHeapAllocator allocator) 
        throws IOException {
        this.fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        this.allocator = allocator;
        this.fileSize = fileChannel.size();
    }
    
    /**
     * 读取下一个内存块
     */
    public MemoryBlock readBlock() throws IOException {
        long remaining = fileSize - position;
        if (remaining <= 0) {
            return null;
        }
        
        long blockSize = Math.min(DEFAULT_BLOCK_SIZE, remaining);
        MemoryBlock block = allocator.allocateFromPool(blockSize);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) blockSize);
        int bytesRead = fileChannel.read(buffer, position);
        
        if (bytesRead > 0) {
            position += bytesRead;
            return new FileChannelBlock(buffer, bytesRead, allocator);
        }
        
        block.close();
        return null;
    }
    
    /**
     * 流式读取处理
     */
    public void streamProcess(BlockHandler handler) throws IOException {
        MemoryBlock block;
        while ((block = readBlock()) != null) {
            try {
                handler.handle(block);
            } finally {
                block.close();
            }
        }
    }
    
    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
```

### 6.2 堆外输出流

```java
package cn.itcraft.jxlsb.io;

/**
 * 堆外内存输出流
 * 
 * <p>直接从堆外内存块写入到FileChannel，零拷贝。
 */
public final class OffHeapOutputStream implements AutoCloseable {
    
    private final FileChannel fileChannel;
    private final OffHeapAllocator allocator;
    private long position = 0;
    
    public OffHeapOutputStream(Path path, OffHeapAllocator allocator) 
        throws IOException {
        this.fileChannel = FileChannel.open(path, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.WRITE);
        this.allocator = allocator;
    }
    
    /**
     * 写入内存块到文件
     */
    public void writeBlock(MemoryBlock block) throws IOException {
        long address = block.getAddress();
        long size = block.size();
        
        fileChannel.write(
            ByteBuffer.allocateDirect((int) size)
                .order(ByteOrder.LITTLE_ENDIAN),
            position
        );
        
        position += size;
    }
    
    /**
     * 批量写入
     */
    public void writeBlocks(MemoryBlock[] blocks) throws IOException {
        for (MemoryBlock block : blocks) {
            writeBlock(block);
        }
    }
    
    @Override
    public void close() throws IOException {
        fileChannel.close();
    }
}
```

---

## 七、API层详细设计（流式API）

### 7.1 XlsbReader设计

```java
package cn.itcraft.jxlsb.api;

/**
 * XLSB文件读取器
 * 
 * <p>提供流式API读取XLSB文件，使用Builder模式构建。
 */
public final class XlsbReader implements AutoCloseable {
    
    private final OffHeapAllocator allocator;
    private final OffHeapInputStream inputStream;
    private final RecordParser recordParser;
    private final MemoryPool memoryPool;
    
    private XlsbReader(Builder builder) throws IOException {
        this.allocator = builder.allocator != null ? 
            builder.allocator : createDefaultAllocator();
        this.inputStream = new OffHeapInputStream(builder.path, allocator);
        this.recordParser = new RecordParser(allocator);
        this.memoryPool = new MemoryPool(allocator);
    }
    
    /**
     * 流式读取所有Sheet
     */
    public void readSheets(SheetHandler handler) throws IOException {
        inputStream.streamProcess(block -> {
            recordParser.parseStream(block, record -> {
                if (record instanceof BeginSheetRecord) {
                    OffHeapSheet sheet = createSheet((BeginSheetRecord) record);
                    handler.handle(sheet);
                }
            });
        });
    }
    
    /**
     * 读取指定Sheet
     */
    public OffHeapSheet readSheet(int sheetIndex) throws IOException {
        SheetLocator locator = new SheetLocator(sheetIndex);
        readSheets(locator);
        return locator.getSheet();
    }
    
    /**
     * 读取单元格数据
     */
    public void readCells(int sheetIndex, CellHandler handler) 
        throws IOException {
        OffHeapSheet sheet = readSheet(sheetIndex);
        try {
            sheet.iterator().forEachRemaining(row -> {
                for (int i = 0; i < row.getColumnCount(); i++) {
                    handler.handle(row.getCell(i));
                }
            });
        } finally {
            sheet.close();
        }
    }
    
    @Override
    public void close() throws IOException {
        inputStream.close();
        memoryPool.close();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private Path path;
        private OffHeapAllocator allocator;
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public Builder allocator(OffHeapAllocator allocator) {
            this.allocator = allocator;
            return this;
        }
        
        public XlsbReader build() throws IOException {
            Objects.requireNonNull(path, "Path must not be null");
            return new XlsbReader(this);
        }
    }
}
```

### 7.2 XlsbWriter设计

```java
package cn.itcraft.jxlsb.api;

/**
 * XLSB文件写入器
 * 
 * <p>提供流式API写入XLSB文件，使用Builder模式构建。
 */
public final class XlsbWriter implements AutoCloseable {
    
    private final OffHeapAllocator allocator;
    private final OffHeapOutputStream outputStream;
    private final RecordWriter recordWriter;
    private final MemoryPool memoryPool;
    
    private XlsbWriter(Builder builder) throws IOException {
        this.allocator = builder.allocator != null ? 
            builder.allocator : createDefaultAllocator();
        this.outputStream = new OffHeapOutputStream(builder.path, allocator);
        this.recordWriter = new RecordWriter(allocator);
        this.memoryPool = new MemoryPool(allocator);
    }
    
    /**
     * 创建新Sheet
     */
    public OffHeapSheet createSheet(String sheetName, 
                                    int rowCount, int columnCount) {
        return new OffHeapSheet(sheetName, 
                               outputStream.getSheetCount(),
                               rowCount, columnCount, allocator);
    }
    
    /**
     * 写入Sheet数据
     */
    public void writeSheet(OffHeapSheet sheet) throws IOException {
        BeginSheetRecord beginRecord = new BeginSheetRecord(
            allocator.allocateFromPool(4), 4);
        beginRecord.setSheetIndex(sheet.getSheetIndex());
        recordWriter.write(beginRecord);
        
        sheet.iterator().forEachRemaining(row -> {
            for (int i = 0; i < row.getColumnCount(); i++) {
                OffHeapCell cell = row.getCell(i);
                CellRecord cellRecord = new CellRecord(
                    cell.memoryBlock, (int) cell.memoryBlock.size());
                recordWriter.write(cellRecord);
            }
        });
        
        EndSheetRecord endRecord = new EndSheetRecord();
        recordWriter.write(endRecord);
    }
    
    /**
     * 批量写入数据（性能优化）
     */
    public void writeBatch(String sheetName, 
                          CellDataSupplier supplier,
                          int rowCount, int columnCount) 
        throws IOException {
        OffHeapSheet sheet = createSheet(sheetName, rowCount, columnCount);
        
        for (int row = 0; row < rowCount; row++) {
            OffHeapRow currentRow = sheet.createRow(row);
            for (int col = 0; col < columnCount; col++) {
                CellData data = supplier.get(row, col);
                OffHeapCell cell = currentRow.getCell(col);
                setCellData(cell, data);
            }
        }
        
        writeSheet(sheet);
        sheet.close();
    }
    
    @Override
    public void close() throws IOException {
        outputStream.close();
        memoryPool.close();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private Path path;
        private OffHeapAllocator allocator;
        
        public Builder path(Path path) {
            this.path = path;
            return this;
        }
        
        public Builder allocator(OffHeapAllocator allocator) {
            this.allocator = allocator;
            return this;
        }
        
        public XlsbWriter build() throws IOException {
            Objects.requireNonNull(path, "Path must not be null");
            return new XlsbWriter(this);
        }
    }
}
```

### 7.3 函数式接口

```java
package cn.itcraft.jxlsb.api;

/**
 * Sheet处理器接口
 */
@FunctionalInterface
public interface SheetHandler {
    void handle(OffHeapSheet sheet) throws IOException;
}

/**
 * Cell处理器接口
 */
@FunctionalInterface
public interface CellHandler {
    void handle(OffHeapCell cell) throws IOException;
}

/**
 * Record处理器接口
 */
@FunctionalInterface
public interface RecordHandler {
    void handle(BiffRecord record) throws IOException;
}

/**
 * Cell数据供应接口
 */
@FunctionalInterface
public interface CellDataSupplier {
    CellData get(int row, int col);
}

/**
 * Cell数据结构
 */
public final class CellData {
    private final CellType type;
    private final Object value;
    
    public CellData(CellType type, Object value) {
        this.type = type;
        this.value = value;
    }
    
    public static CellData text(String text) {
        return new CellData(CellType.TEXT, text);
    }
    
    public static CellData number(double number) {
        return new CellData(CellType.NUMBER, number);
    }
    
    public static CellData date(long timestamp) {
        return new CellData(CellType.DATE, timestamp);
    }
    
    public static CellData boolean(boolean bool) {
        return new CellData(CellType.BOOLEAN, bool);
    }
}
```

---

## 八、多JDK版本支持策略

### 8.1 ServiceLoader机制

**Java 8基础接口定义：**

```java
package cn.itcraft.jxlsb.spi;

/**
 * 内存分配器服务接口
 * 
 * <p>通过ServiceLoader机制自动加载最优实现。
 */
public interface AllocatorProvider {
    OffHeapAllocator createAllocator();
    String getName();
    int getPriority();
}
```

**Java 8实现（src/main/java）：**

```java
package cn.itcraft.jxlsb.spi.impl;

public final class ByteBufferAllocatorProvider implements AllocatorProvider {
    @Override
    public OffHeapAllocator createAllocator() {
        return new ByteBufferAllocator();
    }
    
    @Override
    public String getName() {
        return "ByteBuffer-Unsafe";
    }
    
    @Override
    public int getPriority() {
        return 10;
    }
}
```

**Java 17实现（src/main/java17）：**

```java
package cn.itcraft.jxlsb.spi.impl;

public final class MemorySegmentAllocatorProvider implements AllocatorProvider {
    @Override
    public OffHeapAllocator createAllocator() {
        return new MemorySegmentAllocator();
    }
    
    @Override
    public String getName() {
        return "MemorySegment-ForeignAPI";
    }
    
    @Override
    public int getPriority() {
        return 20;
    }
}
```

**ServiceLoader配置文件：**

`src/main/resources/META-INF/services/cn.itcraft.jxlsb.spi.AllocatorProvider`：
```
cn.itcraft.jxlsb.spi.impl.ByteBufferAllocatorProvider
```

`src/main/java17/resources/META-INF/services/cn.itcraft.jxlsb.spi.AllocatorProvider`：
```
cn.itcraft.jxlsb.spi.impl.MemorySegmentAllocatorProvider
```

### 8.2 自动加载机制

```java
package cn.itcraft.jxlsb.memory;

/**
 * 内存分配器工厂
 * 
 * <p>根据JDK版本自动加载最优的内存分配器实现。
 */
public final class AllocatorFactory {
    
    private static final AllocatorProvider DEFAULT_PROVIDER;
    
    static {
        DEFAULT_PROVIDER = loadBestProvider();
    }
    
    private static AllocatorProvider loadBestProvider() {
        ServiceLoader<AllocatorProvider> loader = 
            ServiceLoader.load(AllocatorProvider.class);
        
        AllocatorProvider best = null;
        int maxPriority = -1;
        
        for (AllocatorProvider provider : loader) {
            if (provider.getPriority() > maxPriority) {
                maxPriority = provider.getPriority();
                best = provider;
            }
        }
        
        return best != null ? best : new ByteBufferAllocatorProvider();
    }
    
    public static OffHeapAllocator createDefaultAllocator() {
        return DEFAULT_PROVIDER.createAllocator();
    }
    
    public static AllocatorProvider getDefaultProvider() {
        return DEFAULT_PROVIDER;
    }
}
```

---

## 九、测试策略

### 9.1 单元测试

**测试覆盖范围：**
- 内存管理层：内存块分配、读写、边界检查、释放
- 数据结构层：Cell/Row/Sheet创建、数据读写、资源释放
- 格式层：Record解析、序列化、类型识别
- API层：Builder模式、流式处理、异常处理

**测试框架：** JUnit 5 + Mockito

**示例测试类：**

```java
package cn.itcraft.jxlsb.memory;

class OffHeapAllocatorTest {
    
    @Test
    void testAllocateAndRelease() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        
        MemoryBlock block = allocator.allocate(1024);
        assertNotNull(block);
        assertEquals(1024, block.size());
        
        block.putInt(0, 12345);
        assertEquals(12345, block.getInt(0));
        
        block.close();
        assertEquals(0, allocator.getTotalAllocated());
    }
    
    @Test
    void testOutOfBounds() {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        MemoryBlock block = allocator.allocate(64);
        
        assertThrows(IndexOutOfBoundsException.class, 
            () -> block.getInt(100));
        
        block.close();
    }
}
```

### 9.2 集成测试

**真实XLSB文件测试：**
- 使用Excel生成的真实XLSB文件
- 测试读取正确性、性能、内存占用
- 测试写入文件的可读性

**测试文件来源：**
- 小文件（<1MB）：基础功能验证
- 中文件（10MB-100MB）：性能验证
- 大文件（>1GB）：流式处理验证

### 9.3 性能测试

**性能指标：**
- 读速度：MB/s
- 写速度：MB/s
- 内存占用：堆外内存大小、堆内存大小
- GC压力：GC次数、GC时间

**测试工具：** JMH（Java Microbenchmark Harness）

**示例性能测试：**

```java
package cn.itcraft.jxlsb.perf;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class XlsbReaderBenchmark {
    
    private Path testFile;
    private OffHeapAllocator allocator;
    
    @Setup
    void setup() throws IOException {
        testFile = Paths.get("test-data/large.xlsx");
        allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    @Benchmark
    void readLargeFile() throws IOException {
        XlsbReader reader = XlsbReader.builder()
            .path(testFile)
            .allocator(allocator)
            .build();
        
        reader.readCells(0, cell -> {
            CellType type = cell.getType();
        });
        
        reader.close();
    }
}
```

### 9.4 内存泄漏测试

**测试方法：**
- 使用Java Mission Control监控堆外内存
- 使用JConsole监控内存趋势
- 压测循环读写，验证内存是否释放

**示例内存测试：**

```java
package cn.itcraft.jxlsb.memory;

class MemoryLeakTest {
    
    @Test
    void testNoLeakOnRepeatedRead() throws IOException {
        OffHeapAllocator allocator = AllocatorFactory.createDefaultAllocator();
        Path testFile = Paths.get("test-data/100MB.xlsb");
        
        long initialMemory = allocator.getTotalAllocated();
        
        for (int i = 0; i < 100; i++) {
            XlsbReader reader = XlsbReader.builder()
                .path(testFile)
                .allocator(allocator)
                .build();
            
            reader.readCells(0, cell -> {});
            reader.close();
        }
        
        long finalMemory = allocator.getTotalAllocated();
        assertTrue(finalMemory <= initialMemory + 1024);
    }
}
```

---

## 十、性能目标

### 10.1 性能指标

| 指标 | 目标值 | 备注 |
|------|--------|------|
| 读取速度 | >50MB/s | 大文件流式读取 |
| 写入速度 | >30MB/s | 批量写入优化 |
| 堆内存占用 | <10MB | 所有数据在堆外 |
| 堆外内存占用 | 文件大小 * 1.2 | 包含解析缓冲 |
| GC次数 | <5次/GB文件 | 全量堆外内存 |
| 启动时间 | <2秒 | 内存池初始化 |

### 10.2 性能优化策略

1. **零拷贝IO**：FileChannel直接读写堆外内存
2. **内存池**：避免频繁分配释放堆外内存
3. **批量读写**：减少Record写入次数
4. **并行处理**：多Sheet并行解析（可选）
5. **内存预分配**：根据文件大小预估内存池容量

---

## 十一、依赖管理

### 11.1 Maven依赖配置

```xml
<dependencies>
    <!-- 日志API -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    
    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.5.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- 性能测试 -->
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-core</artifactId>
        <version>1.37</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.openjdk.jmh</groupId>
        <artifactId>jmh-generator-annprocess</artifactId>
        <version>1.37</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 11.2 Maven构建配置

```xml
<build>
    <plugins>
        <!-- Java 8基础编译 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>8</source>
                <target>8</target>
                <compilerArgs>
                    <arg>-Xlint:all</arg>
                </compilerArgs>
            </configuration>
        </plugin>
        
        <!-- Java 17高级编译 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <executions>
                <execution>
                    <id>compile-java17</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                    <configuration>
                        <source>17</source>
                        <target>17</target>
                        <compileSourceRoots>
                            <compileSourceRoot>${project.basedir}/src/main/java17</compileSourceRoot>
                        </compileSourceRoots>
                        <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        
        <!-- SpotBugs静态分析 -->
        <plugin>
            <groupId>com.github.spotbugs</groupId>
            <artifactId>spotbugs-maven-plugin</artifactId>
            <version>4.8.0</version>
            <configuration>
                <effort>Max</effort>
                <threshold>Medium</threshold>
            </configuration>
        </plugin>
        
        <!-- PMD静态分析 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-pmd-plugin</artifactId>
            <version>3.21.0</version>
            <configuration>
                <rulesets>
                    <ruleset>/rulesets/java/quickstart.xml</ruleset>
                </rulesets>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 十二、包结构设计

```
cn.itcraft.jxlsb/
├── api/                    # 流式API层
│   ├── XlsbReader.java
│   ├── XlsbWriter.java
│   ├── SheetReader.java
│   ├── SheetWriter.java
│   ├── CellHandler.java
│   ├── SheetHandler.java
│   └── CellData.java
├── data/                   # 数据结构层
│   ├── OffHeapCell.java
│   ├── OffHeapRow.java
│   ├── OffHeapSheet.java
│   ├── OffHeapWorkbook.java
│   └── CellType.java
├── memory/                 # 内存管理层（接口）
│   ├── OffHeapAllocator.java
│   ├── MemoryBlock.java
│   ├── MemoryPool.java
│   ├── AllocatorFactory.java
│   └── impl/               # Java 8实现
│       ├── ByteBufferAllocator.java
│       ├── ByteBufferMemoryBlock.java
│       └── ByteBufferAllocatorProvider.java
├── format/                 # XLSB格式层
│   ├── RecordParser.java
│   ├── RecordWriter.java
│   ├── BiffRecord.java
│   └── record/             # 具体记录类型
│       ├── BeginSheetRecord.java
│       ├── BeginRowRecord.java
│       ├── CellRecord.java
│       ├── StringRecord.java
│       └── EndSheetRecord.java
├── io/                     # IO层
│   ├── OffHeapInputStream.java
│   ├── OffHeapOutputStream.java
│   ├── BlockHandler.java
│   └── FileChannelBlock.java
├── spi/                    # 服务提供者接口
│   ├── AllocatorProvider.java
│   └── impl/
│       ├── ByteBufferAllocatorProvider.java
│       └── MemorySegmentAllocatorProvider.java (Java17)
├── exception/              # 异常定义
│   ├── XlsbException.java
│   ├── RecordParseException.java
│   ├── MemoryAllocationException.java
│   └── UnsupportedCellTypeException.java
└── util/                   # 工具类
    ├── ByteOrderUtils.java
    ├── DateConverter.java
    └── StringPool.java
```

**Java 17特殊包：**
```
src/main/java17/cn.itcraft.jxlsb/
├── memory/impl/
│   ├── MemorySegmentAllocator.java
│   ├── MemorySegmentBlock.java
│   └── MemorySegmentAllocatorProvider.java
└── spi/impl/
    └── MemorySegmentAllocatorProvider.java
```

---

## 十三、异常处理设计

### 13.1 异常层次结构

```java
package cn.itcraft.jxlsb.exception;

/**
 * XLSB库基础异常
 */
public class XlsbException extends RuntimeException {
    public XlsbException(String message) {
        super(message);
    }
    
    public XlsbException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * 记录解析异常
 */
public class RecordParseException extends XlsbException {
    private final int recordType;
    
    public RecordParseException(int recordType, String message) {
        super("Failed to parse record type 0x" + 
              Integer.toHexString(recordType) + ": " + message);
        this.recordType = recordType;
    }
    
    public int getRecordType() {
        return recordType;
    }
}

/**
 * 内存分配异常
 */
public class MemoryAllocationException extends XlsbException {
    private final long requestedSize;
    
    public MemoryAllocationException(long requestedSize, String message) {
        super("Failed to allocate " + requestedSize + " bytes: " + message);
        this.requestedSize = requestedSize;
    }
    
    public long getRequestedSize() {
        return requestedSize;
    }
}

/**
 * 不支持的单元格类型异常
 */
public class UnsupportedCellTypeException extends XlsbException {
    private final int typeCode;
    
    public UnsupportedCellTypeException(int typeCode) {
        super("Unsupported cell type code: 0x" + Integer.toHexString(typeCode));
        this.typeCode = typeCode;
    }
    
    public int getTypeCode() {
        return typeCode;
    }
}
```

---

## 十四、资源安全设计

### 14.1 资源释放机制

**AutoCloseable接口：** 所有持有堆外内存的对象实现AutoCloseable

```java
try (XlsbReader reader = XlsbReader.builder()
        .path(Paths.get("test.xlsb"))
        .build()) {
    reader.readCells(0, cell -> {
        System.out.println(cell.getText());
    });
}
```

**Cleaner机制：** 防止忘记关闭资源导致的内存泄漏

```java
package cn.itcraft.jxlsb.memory;

/**
 * 内存块资源清理器
 * 
 * <p>使用Cleaner确保堆外内存最终被释放，防止内存泄漏。
 */
final class MemoryBlockCleaner {
    
    private static final Cleaner cleaner = Cleaner.create();
    
    static void register(MemoryBlock block, Runnable cleanupAction) {
        cleaner.register(block, cleanupAction);
    }
}
```

---

## 十五、示例代码

### 15.1 读取示例

```java
package cn.itcraft.jxlsb.example;

/**
 * XLSB读取示例
 */
public final class ReadExample {
    
    public static void main(String[] args) throws IOException {
        Path file = Paths.get("data.xlsx");
        
        try (XlsbReader reader = XlsbReader.builder()
                .path(file)
                .build()) {
            
            reader.readSheets(sheet -> {
                System.out.println("Sheet: " + sheet.getSheetName());
                
                sheet.iterator().forEachRemaining(row -> {
                    System.out.println("Row " + row.getRowIndex());
                    
                    for (int i = 0; i < row.getColumnCount(); i++) {
                        OffHeapCell cell = row.getCell(i);
                        System.out.println("  Col " + i + ": " + 
                            formatCell(cell));
                    }
                });
                
                sheet.close();
            });
        }
    }
    
    private static String formatCell(OffHeapCell cell) {
        switch (cell.getType()) {
            case TEXT: return cell.getText();
            case NUMBER: return String.valueOf(cell.getNumber());
            case DATE: return formatDate(cell.getDate());
            case BOOLEAN: return String.valueOf(cell.getBoolean());
            default: return "";
        }
    }
}
```

### 15.2 写入示例

```java
package cn.itcraft.jxlsb.example;

/**
 * XLSB写入示例
 */
public final class WriteExample {
    
    public static void main(String[] args) throws IOException {
        Path file = Paths.get("output.xlsx");
        
        try (XlsbWriter writer = XlsbWriter.builder()
                .path(file)
                .build()) {
            
            writer.writeBatch("Sheet1", 
                (row, col) -> generateData(row, col),
                1000, 100);
        }
    }
    
    private static CellData generateData(int row, int col) {
        if (col == 0) {
            return CellData.text("Row-" + row);
        } else if (col == 1) {
            return CellData.number(row * 100.5);
        } else {
            return CellData.date(System.currentTimeMillis());
        }
    }
}
```

---

## 十六、开发里程碑

### Phase 1: 基础框架（预计2周）
- 内存管理层：Java 8实现
- 数据结构层：Cell/Row/Sheet基础结构
- IO层：FileChannel零拷贝读写
- 基础单元测试

### Phase 2: XLSB格式解析（预计3周）
- RecordParser实现
- BIFF12记录类型实现（10+核心记录）
- 格式层测试
- 集成测试（真实XLSB文件）

### Phase 3: API层（预计2周）
- XlsbReader/XlsbWriter流式API
- Builder模式实现
- 函数式接口定义
- API层测试

### Phase 4: Java 17支持（预计1周）
- MemorySegmentAllocator实现
- ServiceLoader机制
- 多JDK版本测试

### Phase 5: 性能优化（预计1周）
- 内存池优化
- 批量读写优化
- JMH性能测试
- 内存泄漏测试

### Phase 6: 文档与发布（预计1周）
- API文档完善
- 使用示例编写
- README.md
- Maven Central发布准备

---

## 十七、风险与挑战

### 17.1 技术风险

| 风险 | 影响 | 缓解策略 |
|------|------|----------|
| Unsafe安全限制 | Java 9+限制Unsafe访问 | Java 17实现MemorySegment替代 |
| 堆外内存泄漏 | 内存耗尽 | Cleaner机制、严格测试 |
| XLSB格式复杂 | 解析错误 | 参考[MS-XLSB]规范、增量实现 |
| 大文件处理 | 性能瓶颈 | 流式处理、内存池优化 |

### 17.2 缓解措施

1. **Unsafe限制：** Java 17使用Foreign Memory API，Java 8使用ByteBuffer
2. **内存泄漏：** AutoCloseable + Cleaner + 严格测试
3. **格式复杂：** 先实现核心记录，逐步扩展
4. **大文件：** 流式处理，避免全量加载

---

## 附录A：[MS-XLSB]关键记录参考

基于[MS-XLSB].pdf规范，以下为核心记录类型：

**Workbook级记录：**
- BR_BEGINBOOK (0x0808)：Workbook开始
- BR_VERSION (0x0080)：版本信息
- BR_BEGINBUNDLESHS (0x0841)：Sheet集合开始
- BR_ENDBOOK (0x0809)：Workbook结束

**Sheet级记录：**
- BR_BEGINSHEET (0x0085)：Sheet开始
- BR_ENDSHEET (0x0086)：Sheet结束
- BR_BEGINWSVIEW (0x0885)：视图信息

**Row/Cell级记录：**
- BR_BEGINROW (0x0087)：Row开始
- BR_ENDROW (0x0088)：Row结束
- BR_CELL (0x0143)：Cell数据
- BR_STRING (0x00F9)：字符串数据

---

**设计文档版本：** 2026-04-10 v1.0  
**作者：** AI架构师  
**状态：** 待审核