package com.huseyin.couriertracking.exception;

import java.time.Instant;

public class FutureTimeException extends RuntimeException {
    public FutureTimeException(Instant time, Instant now) {
        super("Location time " + time + " is in the future (server time " + now + ")");
    }
}
