package com.peatroxd.mtprototest.checker.model;

public record MtProtoDeepProbeResult(
        boolean success,
        boolean resPqReceived,
        long latencyMs,
        MtProtoProbeFailureCode failureCode,
        String reason
) {
    public static MtProtoDeepProbeResult success(long latencyMs) {
        return new MtProtoDeepProbeResult(true, true, latencyMs, null, null);
    }

    public static MtProtoDeepProbeResult failure(MtProtoProbeFailureCode failureCode, String reason) {
        return new MtProtoDeepProbeResult(false, false, -1, failureCode, reason);
    }
}
