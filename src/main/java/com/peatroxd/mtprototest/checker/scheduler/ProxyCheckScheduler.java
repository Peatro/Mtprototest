package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckCycleTrackingService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckRunCoordinator;
import com.peatroxd.mtprototest.checker.service.ProxyRetentionService;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyCheckScheduler {

    private static final String SCHEDULED_TRIGGER = "scheduled";

    private final ProxyBatchCheckService proxyBatchCheckService;
    private final ProxyRetentionService proxyRetentionService;
    private final ProxyCheckRunCoordinator proxyCheckRunCoordinator;
    private final ProxyCheckCycleTrackingService proxyCheckCycleTrackingService;
    private final ProxyMetricsService proxyMetricsService;

    @Scheduled(
            initialDelayString = "${app.checker.initial-delay-ms:300000}",
            fixedDelayString = "${app.checker.fixed-delay-ms:300000}"
    )
    public void checkNewProxies() {
        if (!proxyCheckRunCoordinator.tryStartCatalogCycle("scheduled proxy check")) {
            proxyCheckCycleTrackingService.markSkipped(SCHEDULED_TRIGGER, "Previous scheduled or startup cycle is still active");
            proxyMetricsService.incrementCheckCycleSkipped(SCHEDULED_TRIGGER);
            return;
        }

        proxyCheckCycleTrackingService.markStarted(SCHEDULED_TRIGGER);
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            log.info("Starting scheduled proxy check");
            int archivedCount = proxyRetentionService.archiveStaleDeadProxies();
            ProxyBatchCheckSummary newSummary = proxyBatchCheckService.checkNewProxies();
            ProxyBatchCheckSummary aliveQuickOkSummary = proxyBatchCheckService.checkAliveQuickOkProxies();
            ProxyBatchCheckSummary aliveVerifiedSummary = proxyBatchCheckService.checkAliveVerifiedProxies();
            ProxyBatchCheckSummary deadSummary = proxyBatchCheckService.checkDeadProxies();
            log.info(
                    "Scheduled proxy check finished: archived={}, new(total={}, quickOk={}, verified={}, dead={}), aliveQuickOk(total={}, quickOk={}, verified={}, dead={}), aliveVerified(total={}, quickOk={}, verified={}, dead={}), dead(total={}, quickOk={}, verified={}, dead={})",
                    archivedCount,
                    newSummary.totalChecked(),
                    newSummary.quickOkCount(),
                    newSummary.verifiedCount(),
                    newSummary.deadCount(),
                    aliveQuickOkSummary.totalChecked(),
                    aliveQuickOkSummary.quickOkCount(),
                    aliveQuickOkSummary.verifiedCount(),
                    aliveQuickOkSummary.deadCount(),
                    aliveVerifiedSummary.totalChecked(),
                    aliveVerifiedSummary.quickOkCount(),
                    aliveVerifiedSummary.verifiedCount(),
                    aliveVerifiedSummary.deadCount(),
                    deadSummary.totalChecked(),
                    deadSummary.quickOkCount(),
                    deadSummary.verifiedCount(),
                    deadSummary.deadCount()
            );
            int totalChecked = newSummary.totalChecked()
                    + aliveQuickOkSummary.totalChecked()
                    + aliveVerifiedSummary.totalChecked()
                    + deadSummary.totalChecked();
            proxyCheckCycleTrackingService.markFinished(SCHEDULED_TRIGGER, totalChecked, archivedCount);
            proxyMetricsService.recordCheckCycleDuration(
                    SCHEDULED_TRIGGER,
                    "lifecycle",
                    Duration.between(startedAt, LocalDateTime.now()),
                    true
            );
        } catch (RuntimeException e) {
            proxyCheckCycleTrackingService.markFailed(SCHEDULED_TRIGGER, e.getMessage());
            proxyMetricsService.recordCheckCycleDuration(
                    SCHEDULED_TRIGGER,
                    "lifecycle",
                    Duration.between(startedAt, LocalDateTime.now()),
                    false
            );
            throw e;
        } finally {
            proxyCheckRunCoordinator.finishCatalogCycle();
        }
    }
}
