package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.memory.MemoryBlock;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.data.CellType;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class MemoryBenchmark {
    
    private OffHeapAllocator allocator;
    
    @Setup
    public void setup() {
        allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    @Benchmark
    public void allocateAndFreeBlock() {
        MemoryBlock block = allocator.allocate(1024);
        block.close();
    }
    
    @Benchmark
    public void allocateAndFreeLargeBlock() {
        MemoryBlock block = allocator.allocate(64 * 1024);
        block.close();
    }
    
    @Benchmark
    public void writeAndReadInt() {
        MemoryBlock block = allocator.allocate(64);
        block.putInt(0, 12345);
        int value = block.getInt(0);
        block.close();
    }
    
    @Benchmark
    public void writeAndReadDouble() {
        MemoryBlock block = allocator.allocate(64);
        block.putDouble(0, 3.14159);
        double value = block.getDouble(0);
        block.close();
    }
    
    @Benchmark
    public void createAndPopulateRow() {
        OffHeapRow row = new OffHeapRow(0, 10, allocator);
        
        for (int i = 0; i < 10; i++) {
            OffHeapCell cell = row.getCell(i);
            cell.setNumber(i * 1.5);
        }
        
        row.close();
    }
    
    @Benchmark
    public void createAndReadRow() {
        OffHeapRow row = new OffHeapRow(0, 10, allocator);
        
        for (int i = 0; i < 10; i++) {
            OffHeapCell cell = row.getCell(i);
            cell.setNumber(i * 1.5);
        }
        
        for (int i = 0; i < 10; i++) {
            OffHeapCell cell = row.getCell(i);
            double value = cell.getNumber();
        }
        
        row.close();
    }
}