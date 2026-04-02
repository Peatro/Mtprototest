package com.peatroxd.mtprototest.checker.model;

public record ProxyBatchCheckSummary(
        int totalChecked,
        int quickOkCount,
        int verifiedCount,
        int deadCount
) {
    public static ProxyBatchCheckSummary empty() {
        return new ProxyBatchCheckSummary(0, 0, 0, 0);
    }
}
