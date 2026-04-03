package com.peatroxd.mtprototest.bootstrap;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckCycleTrackingService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckRunCoordinator;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import com.peatroxd.mtprototest.parser.service.ProxyImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.startup", name = "bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class ProxyStartupBootstrap implements ApplicationRunner {

    private static final String STARTUP_TRIGGER = "startup";

    private final ProxyImportService proxyImportService;
    private final ProxyBatchCheckService proxyBatchCheckService;
    private final ProxyCheckRunCoordinator proxyCheckRunCoordinator;
    private final ProxyCheckCycleTrackingService proxyCheckCycleTrackingService;
    private final ProxyMetricsService proxyMetricsService;
    private final StartupProperties startupProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        log.info("Starting proxy bootstrap on application startup");

        proxyImportService.importAll();

        if (!proxyCheckRunCoordinator.tryStartCatalogCycle("startup proxy bootstrap")) {
            proxyCheckCycleTrackingService.markSkipped(STARTUP_TRIGGER, "Another proxy catalog cycle is already active");
            proxyMetricsService.incrementCheckCycleSkipped(STARTUP_TRIGGER);
            return;
        }

        proxyCheckCycleTrackingService.markStarted(STARTUP_TRIGGER);
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            ProxyBatchCheckSummary summary = proxyBatchCheckService.checkStartupProxies(
                    startupProperties.getCheckBatchSize(),
                    startupProperties.getDeepProbeLimit()
            );
            proxyCheckCycleTrackingService.markFinished(STARTUP_TRIGGER, summary.totalChecked(), 0);
            proxyMetricsService.recordCheckCycleDuration(
                    STARTUP_TRIGGER,
                    "startup_candidate_proxies",
                    Duration.between(startedAt, LocalDateTime.now()),
                    true
            );
            log.info(
                    "Startup proxy bootstrap finished: total={}, quickOk={}, verified={}, dead={}, batchSize={}, deepProbeLimit={}",
                    summary.totalChecked(),
                    summary.quickOkCount(),
                    summary.verifiedCount(),
                    summary.deadCount(),
                    startupProperties.getCheckBatchSize(),
                    startupProperties.getDeepProbeLimit()
            );
        } catch (RuntimeException e) {
            proxyCheckCycleTrackingService.markFailed(STARTUP_TRIGGER, e.getMessage());
            proxyMetricsService.recordCheckCycleDuration(
                    STARTUP_TRIGGER,
                    "startup_candidate_proxies",
                    Duration.between(startedAt, LocalDateTime.now()),
                    false
            );
            throw e;
        } finally {
            proxyCheckRunCoordinator.finishCatalogCycle();
        }
    }
}
