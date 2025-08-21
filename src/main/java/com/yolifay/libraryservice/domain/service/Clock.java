package com.yolifay.libraryservice.domain.service;

import java.time.Instant;

public interface Clock {
    Instant now();
}
