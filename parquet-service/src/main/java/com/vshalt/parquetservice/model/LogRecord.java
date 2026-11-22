package com.vshalt.parquetservice.model;

import java.time.Instant;

public record LogRecord(String uuid, Instant time, String service, String level, String message) {
}
