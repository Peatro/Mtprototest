package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.service.CheckCycleSnapshot;
import com.peatroxd.mtprototest.checker.service.ProxyCheckCycleTrackingService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryProxyCheckCycleTrackingService implements ProxyCheckCycleTrackingService {

    private final Map<String, CheckCycleSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void markStarted(String trigger) {
        snapshots.compute(trigger, (ignored, existing) -> new CheckCycleSnapshot(
                trigger,
                LocalDateTime.now(),
                existing != null ? existing.completedAt() : null,
                existing != null && existing.succeeded(),
                existing != null ? existing.totalChecked() : 0,
                existing != null ? existing.archivedCount() : 0,
                false,
                null,
                existing != null ? existing.duration() : null
        ));
    }

    @Override
    public void markFinished(String trigger, int totalChecked, int archivedCount) {
        snapshots.compute(trigger, (ignored, existing) -> {
            LocalDateTime startedAt = existing != null && existing.startedAt() != null ? existing.startedAt() : LocalDateTime.now();
            LocalDateTime completedAt = LocalDateTime.now();
            return new CheckCycleSnapshot(
                    trigger,
                    startedAt,
                    completedAt,
                    true,
                    totalChecked,
                    archivedCount,
                    false,
                    null,
                    Duration.between(startedAt, completedAt)
            );
        });
    }

    @Override
    public void markFailed(String trigger, String message) {
        snapshots.compute(trigger, (ignored, existing) -> {
            LocalDateTime startedAt = existing != null && existing.startedAt() != null ? existing.startedAt() : LocalDateTime.now();
            LocalDateTime completedAt = LocalDateTime.now();
            return new CheckCycleSnapshot(
                    trigger,
                    startedAt,
                    completedAt,
                    false,
                    existing != null ? existing.totalChecked() : 0,
                    existing != null ? existing.archivedCount() : 0,
                    false,
                    message,
                    Duration.between(startedAt, completedAt)
            );
        });
    }

    @Override
    public void markSkipped(String trigger, String message) {
        snapshots.compute(trigger, (ignored, existing) -> new CheckCycleSnapshot(
                trigger,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                existing != null ? existing.totalChecked() : 0,
                existing != null ? existing.archivedCount() : 0,
                true,
                message,
                Duration.ZERO
        ));
    }

    @Override
    public Optional<CheckCycleSnapshot> latestSuccessfulCycle() {
        return snapshots.values().stream()
                .filter(CheckCycleSnapshot::succeeded)
                .max(Comparator.comparing(CheckCycleSnapshot::completedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @Override
    public Optional<CheckCycleSnapshot> latestCycle() {
        return snapshots.values().stream()
                .max(Comparator.comparing(CheckCycleSnapshot::completedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
