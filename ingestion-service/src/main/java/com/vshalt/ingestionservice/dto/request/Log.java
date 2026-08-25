package com.vshalt.ingestionservice.dto.request;

import java.time.Instant;

public record Log(String service, String level, String message, Instant timestamp) { }
