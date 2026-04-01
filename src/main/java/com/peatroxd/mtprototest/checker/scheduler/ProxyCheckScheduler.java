package com.peatroxd.mtprototest.checker.scheduler;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyCheckScheduler {

    private final ProxyBatchCheckService proxyBatchCheckService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${app.checker.fixed-delay-ms:300000}")
    public void checkNewProxies() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipping scheduled proxy check because previous run is still active");
            return;
        }

        try {
            log.info("Starting scheduled proxy check");
            ProxyBatchCheckSummary summary = proxyBatchCheckService.checkNewProxies();
            log.info(
                    "Scheduled proxy check finished: total={}, alive={}, dead={}",
                    summary.totalChecked(),
                    summary.aliveCount(),
                    summary.deadCount()
            );
        } finally {
            running.set(false);
        }
    }
}
