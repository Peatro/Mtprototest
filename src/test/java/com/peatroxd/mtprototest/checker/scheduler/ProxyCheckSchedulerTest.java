package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckCycleTrackingService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckRunCoordinator;
import com.peatroxd.mtprototest.checker.service.ProxyRetentionService;
import com.peatroxd.mtprototest.common.metrics.ProxyMetricsService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProxyCheckSchedulerTest {

    @Test
    void shouldSkipScheduledRunWhenAnotherCatalogCycleIsActive() {
        ProxyBatchCheckService proxyBatchCheckService = mock(ProxyBatchCheckService.class);
        ProxyRetentionService proxyRetentionService = mock(ProxyRetentionService.class);
        ProxyCheckCycleTrackingService proxyCheckCycleTrackingService = mock(ProxyCheckCycleTrackingService.class);
        ProxyMetricsService proxyMetricsService = mock(ProxyMetricsService.class);
        ProxyCheckRunCoordinator coordinator = new ProxyCheckRunCoordinator();
        ProxyCheckScheduler scheduler = new ProxyCheckScheduler(
                proxyBatchCheckService,
                proxyRetentionService,
                coordinator,
                proxyCheckCycleTrackingService,
                proxyMetricsService
        );

        coordinator.tryStartCatalogCycle("test");

        scheduler.checkNewProxies();

        verify(proxyRetentionService, never()).archiveStaleDeadProxies();
        verify(proxyBatchCheckService, never()).checkNewProxies();
        verify(proxyBatchCheckService, never()).checkAliveQuickOkProxies();
        verify(proxyBatchCheckService, never()).checkAliveVerifiedProxies();
        verify(proxyBatchCheckService, never()).checkDeadProxies();
    }
}
