package com.vshalt.parquetservice.writer;

import com.vshalt.parquetservice.config.ParquetProperties;
import com.vshalt.parquetservice.model.LogRecord;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ParquetBatchWriter {

    private static final String PARTITION_DIR_PREFIX = "dt=";

    private final DuckDBConnection root;
    private final Path outputDir;
    private final String compression;

    public ParquetBatchWriter(DuckDBConnection root, ParquetProperties props) {
        this.root = root;
        this.outputDir = props.outputDir();
        this.compression = props.compression().name();
    }

    public List<Path> write(List<LogRecord> records) throws SQLException, IOException {
        if (records.isEmpty()) {
            return List.of();
        }

        String table = "batch_" + UUID.randomUUID().toString().replace("-", "");
        String batchId = UUID.randomUUID().toString();
        long createdAt = System.currentTimeMillis();
        List<Path> written = new ArrayList<>();

        try (DuckDBConnection conn = root.duplicate(); Statement st = conn.createStatement()) {
            try {
                st.execute("CREATE TABLE " + table + " (uuid VARCHAR, time TIMESTAMP, service VARCHAR, "
                        + "level VARCHAR, message VARCHAR)");
                append(conn, table, records);

                for (String dt : distinctDates(st, table)) {
                    Path dir = outputDir.resolve(PARTITION_DIR_PREFIX + dt);
                    Files.createDirectories(dir);

                    String name = "part-" + createdAt + "-" + batchId;
                    Path tmp = dir.resolve("." + name + ".tmp");
                    Path published = dir.resolve(name + ".parquet");

                    try {
                        st.execute("COPY (SELECT uuid, time, service, level, message FROM " + table
                                + " WHERE CAST(time AS DATE) = DATE '" + dt + "' ORDER BY time) TO '"
                                + sqlString(tmp) + "' (FORMAT parquet, COMPRESSION " + compression + ")");
                        Files.move(tmp, published, StandardCopyOption.ATOMIC_MOVE);
                    } finally {
                        Files.deleteIfExists(tmp);
                    }
                    written.add(published);
                }
            } finally {
                st.execute("DROP TABLE IF EXISTS " + table);
            }
        }
        return written;
    }

    private static void append(DuckDBConnection conn, String table, List<LogRecord> records) throws SQLException {
        try (DuckDBAppender appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, table)) {
            for (LogRecord r : records) {
                appender.beginRow();
                appender.append(r.uuid());
                appender.appendEpochMicros(toEpochMicros(r.time()));
                appender.append(r.service());
                appender.append(r.level());
                appender.append(r.message());
                appender.endRow();
            }
            appender.flush();
        }
    }

    private static List<String> distinctDates(Statement st, String table) throws SQLException {
        List<String> dates = new ArrayList<>();
        try (ResultSet rs = st.executeQuery("SELECT DISTINCT strftime(CAST(time AS DATE), '%Y-%m-%d') FROM " + table)) {
            while (rs.next()) {
                dates.add(rs.getString(1));
            }
        }
        return dates;
    }

    private static long toEpochMicros(Instant t) {
        return Math.addExact(Math.multiplyExact(t.getEpochSecond(), 1_000_000L), t.getNano() / 1_000L);
    }

    private static String sqlString(Path path) {
        return path.toAbsolutePath().toString().replace("'", "''");
    }
}
