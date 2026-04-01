package com.peatroxd.mtprototest.checker.model;

public record ProxyCheckResult(
        boolean alive,
        long latencyMs,
        String errorMessage
) {
}
