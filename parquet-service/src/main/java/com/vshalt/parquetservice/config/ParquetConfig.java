package com.vshalt.parquetservice.config;

import org.duckdb.DuckDBConnection;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(ParquetProperties.class)
public class ParquetConfig {

    @Bean(destroyMethod = "close")
    public DuckDBConnection duckDb(ParquetProperties props) throws SQLException {
        Properties config = new Properties();
        config.setProperty("threads", String.valueOf(props.duckdbThreads()));
        config.setProperty("memory_limit", props.duckdbMemoryLimit());
        return (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:", config);
    }
}
