package com.peatroxd.mtprototest.checker.service;

import java.time.Duration;
import java.time.LocalDateTime;

public record CheckCycleSnapshot(
        String trigger,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        boolean succeeded,
        int totalChecked,
        int archivedCount,
        boolean skipped,
        String message,
        Duration duration
) {
}
