package com.peatroxd.mtprototest.common.health;

import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import com.peatroxd.mtprototest.checker.service.CheckCycleSnapshot;
import com.peatroxd.mtprototest.checker.service.ProxyCheckCycleTrackingService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component("proxyChecker")
public class ProxyCheckerHealthIndicator implements HealthIndicator {

    private final ProxyCheckCycleTrackingService proxyCheckCycleTrackingService;
    private final CheckerProperties checkerProperties;

    public ProxyCheckerHealthIndicator(
            ProxyCheckCycleTrackingService proxyCheckCycleTrackingService,
            CheckerProperties checkerProperties
    ) {
        this.proxyCheckCycleTrackingService = proxyCheckCycleTrackingService;
        this.checkerProperties = checkerProperties;
    }

    @Override
    public Health health() {
        Optional<CheckCycleSnapshot> latestCycle = proxyCheckCycleTrackingService.latestCycle();
        Optional<CheckCycleSnapshot> latestSuccessfulCycle = proxyCheckCycleTrackingService.latestSuccessfulCycle();

        if (latestCycle.isEmpty()) {
            return Health.unknown()
                    .withDetail("reason", "No checker cycles recorded yet")
                    .build();
        }

        CheckCycleSnapshot snapshot = latestCycle.orElseThrow();
        Duration staleThreshold = Duration.ofMillis(Math.max(checkerProperties.getFixedDelayMs() * 3, checkerProperties.getFixedDelayMs() + 60_000));
        LocalDateTime latestSuccessCompletedAt = latestSuccessfulCycle.map(CheckCycleSnapshot::completedAt).orElse(null);

        Health.Builder builder;
        if (snapshot.skipped()) {
            builder = Health.down();
        } else if (!snapshot.succeeded()) {
            builder = Health.down();
        } else if (latestSuccessCompletedAt == null || latestSuccessCompletedAt.isBefore(LocalDateTime.now().minus(staleThreshold))) {
            builder = Health.down();
        } else {
            builder = Health.up();
        }

        return builder
                .withDetail("latestTrigger", snapshot.trigger())
                .withDetail("latestSucceeded", snapshot.succeeded())
                .withDetail("latestSkipped", snapshot.skipped())
                .withDetail("latestCompletedAt", snapshot.completedAt())
                .withDetail("latestDurationMs", snapshot.duration() != null ? snapshot.duration().toMillis() : -1)
                .withDetail("latestTotalChecked", snapshot.totalChecked())
                .withDetail("latestArchivedCount", snapshot.archivedCount())
                .withDetail("latestMessage", snapshot.message() != null ? snapshot.message() : "")
                .withDetail("staleThresholdMs", staleThreshold.toMillis())
                .withDetail("latestSuccessfulCompletedAt", latestSuccessCompletedAt != null ? latestSuccessCompletedAt : "none")
                .build();
    }
}
