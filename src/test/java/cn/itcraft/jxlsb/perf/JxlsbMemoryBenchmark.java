package cn.itcraft.jxlsb.perf;

import cn.itcraft.jxlsb.api.CellData;
import cn.itcraft.jxlsb.data.OffHeapRow;
import cn.itcraft.jxlsb.data.OffHeapCell;
import cn.itcraft.jxlsb.memory.OffHeapAllocator;
import cn.itcraft.jxlsb.memory.AllocatorFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.*;

/**
 * jxlsb内存性能基准测试
 * 
 * <p>测试jxlsb堆外内存数据结构的性能。
 * 
 * @author AI架构师
 * @since 1.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(3)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Threads(4)
public class JxlsbMemoryBenchmark {
    
    @Param({"10000", "100000"})
    int rowCount;
    
    @Param({"10"})
    int columnCount;
    
    private OffHeapAllocator allocator;
    
    @Setup
    public void setup() {
        allocator = AllocatorFactory.createDefaultAllocator();
    }
    
    @Benchmark
    public void createRowsWithJxlsb(Blackhole bh) {
        for (int row = 0; row < rowCount; row++) {
            OffHeapRow offHeapRow = new OffHeapRow(row, columnCount, allocator);
            
            for (int col = 0; col < columnCount; col++) {
                OffHeapCell cell = offHeapRow.getCell(col);
                switch (col % 4) {
                    case 0:
                        cell.setText("Product-" + row + "-" + col);
                        break;
                    case 1:
                        cell.setNumber(row * 100.50 + col);
                        break;
                    case 2:
                        cell.setDate(System.currentTimeMillis());
                        break;
                    case 3:
                        cell.setBoolean(row % 2 == 0);
                        break;
                }
            }
            
            bh.consume(offHeapRow);
            offHeapRow.close();
        }
    }
    
    @Benchmark
    public void createRowsWithPOI(Blackhole bh) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            for (int row = 0; row < rowCount; row++) {
                Row excelRow = sheet.createRow(row);
                
                for (int col = 0; col < columnCount; col++) {
                    Cell cell = excelRow.createCell(col);
                    switch (col % 4) {
                        case 0:
                            cell.setCellValue("Product-" + row + "-" + col);
                            break;
                        case 1:
                            cell.setCellValue(row * 100.50 + col);
                            break;
                        case 2:
                            cell.setCellValue(new Date(System.currentTimeMillis()));
                            break;
                        case 3:
                            cell.setCellValue(row % 2 == 0);
                            break;
                    }
                }
            }
            
            bh.consume(workbook);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Benchmark
    public void readRowsWithJxlsb(Blackhole bh) {
        // 先创建数据
        OffHeapRow[] rows = new OffHeapRow[rowCount];
        for (int row = 0; row < rowCount; row++) {
            rows[row] = new OffHeapRow(row, columnCount, allocator);
            
            for (int col = 0; col < columnCount; col++) {
                OffHeapCell cell = rows[row].getCell(col);
                cell.setNumber(row * 100.0 + col);
            }
        }
        
        // 读取数据
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < columnCount; col++) {
                OffHeapCell cell = rows[row].getCell(col);
                bh.consume(cell.getNumber());
            }
        }
        
        // 清理
        for (int row = 0; row < rowCount; row++) {
            rows[row].close();
        }
    }
    
    @Benchmark
    public void memoryAllocationBenchmark(Blackhole bh) {
        for (int i = 0; i < 1000; i++) {
            OffHeapRow row = new OffHeapRow(i, 10, allocator);
            bh.consume(row);
            row.close();
        }
    }
}