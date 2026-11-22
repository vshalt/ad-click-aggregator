package com.vshalt.parquetservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.parquet")
public record ParquetProperties(
        Path outputDir,
        Compression compression,
        int duckdbThreads,
        String duckdbMemoryLimit
) {
    public enum Compression {
        UNCOMPRESSED, SNAPPY, GZIP, ZSTD, LZ4, BROTLI
    }
}
