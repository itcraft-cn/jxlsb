#!/bin/bash
# Run JMH Benchmark for jxlsb

PROJECT_DIR=$(dirname "$0")
cd "$PROJECT_DIR"

# Compile
mvnd clean compile test-compile -q

# Run File Size Comparison
echo ""
echo "Running File Size Comparison..."
echo ""
java -cp "$(mvnd dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q):target/test-classes:target/classes" \
    cn.itcraft.jxlsb.perf.FileSizeComparisonMain

# Run JMH Benchmark (quick version)
echo ""
echo "Running JMH Performance Benchmark (quick test)..."
echo ""
java -cp "$(mvnd dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q):target/test-classes:target/classes" \
    org.openjdk.jmh.Main ExcelLibraryComparisonBenchmark -wi 3 -i 3 -t 1 -f 1