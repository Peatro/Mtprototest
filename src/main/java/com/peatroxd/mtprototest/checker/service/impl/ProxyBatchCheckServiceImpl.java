package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.model.MtProtoDeepProbeResult;
import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.service.MtProtoDeepProbeService;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.checker.service.ProxyConnectivityChecker;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyBatchCheckServiceImpl implements ProxyBatchCheckService {

    private static final int DEEP_PROBE_LIMIT = 20;

    private final ProxyRepository proxyRepository;
    private final ProxyConnectivityChecker proxyConnectivityChecker;
    private final MtProtoDeepProbeService mtProtoDeepProbeService;
    private final ProxyCheckUpdateService proxyCheckUpdateService;
    private final ExecutorService proxyCheckerExecutor;

    @Override
    public ProxyBatchCheckSummary checkNewProxies() {
        List<ProxyEntity> proxies = proxyRepository.findTop200ByStatusOrderByIdAsc(ProxyStatus.NEW);

        if (proxies.isEmpty()) {
            log.info("No NEW proxies found for checking");
            return ProxyBatchCheckSummary.empty();
        }

        log.info("Checking {} NEW proxies asynchronously", proxies.size());

        AtomicInteger quickOkCount = new AtomicInteger();
        AtomicInteger verifiedCount = new AtomicInteger();
        AtomicInteger deadCount = new AtomicInteger();
        AtomicInteger deepProbesStarted = new AtomicInteger();

        List<CompletableFuture<Void>> futures = proxies.stream()
                .map(proxy -> CompletableFuture
                        .supplyAsync(() -> checkProxy(proxy, deepProbesStarted), proxyCheckerExecutor)
                        .exceptionally(error -> failedResult(proxy, error))
                        .thenAccept(result -> {
                            proxyCheckUpdateService.applyResult(proxy.getId(), result);

                            if (!result.alive()) {
                                deadCount.incrementAndGet();
                            } else if (result.verificationStatus() == ProxyVerificationStatus.VERIFIED) {
                                verifiedCount.incrementAndGet();
                            } else {
                                quickOkCount.incrementAndGet();
                            }
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        ProxyBatchCheckSummary summary = new ProxyBatchCheckSummary(
                proxies.size(),
                quickOkCount.get(),
                verifiedCount.get(),
                deadCount.get()
        );

        log.info(
                "Async proxy check completed: total={}, quickOk={}, verified={}, dead={}",
                summary.totalChecked(),
                summary.quickOkCount(),
                summary.verifiedCount(),
                summary.deadCount()
        );

        return summary;
    }

    private ProxyCheckResult checkProxy(ProxyEntity proxy, AtomicInteger deepProbesStarted) {
        ProxyCheckResult quickResult = proxyConnectivityChecker.check(proxy);

        if (!quickResult.alive()) {
            return quickResult;
        }

        if (deepProbesStarted.getAndIncrement() >= DEEP_PROBE_LIMIT) {
            return quickResult;
        }

        MtProtoDeepProbeResult deepResult = mtProtoDeepProbeService.probe(proxy);
        if (!deepResult.success()) {
            log.debug(
                    "Deep probe did not verify proxyId={}: code={}, reason={}",
                    proxy.getId(),
                    deepResult.failureCode(),
                    deepResult.reason()
            );
            return new ProxyCheckResult(
                    true,
                    quickResult.latencyMs(),
                    ProxyVerificationStatus.QUICK_OK,
                    deepResult.reason()
            );
        }

        long latencyMs = deepResult.latencyMs() >= 0 ? deepResult.latencyMs() : quickResult.latencyMs();
        return new ProxyCheckResult(true, latencyMs, ProxyVerificationStatus.VERIFIED, null);
    }

    private ProxyCheckResult failedResult(ProxyEntity proxy, Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;

        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        log.warn("Connectivity check failed for proxyId={}: {}", proxy.getId(), message);
        return new ProxyCheckResult(false, -1, ProxyVerificationStatus.UNVERIFIED, message);
    }
}
