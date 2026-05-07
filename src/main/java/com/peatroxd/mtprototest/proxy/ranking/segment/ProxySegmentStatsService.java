package com.peatroxd.mtprototest.proxy.ranking.segment;

import com.peatroxd.mtprototest.proxy.ranking.ClientSegment;
import com.peatroxd.mtprototest.proxy.ranking.ProxyRankingProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxySegmentStatsService {

    private final ProxySegmentStatsRepository repository;
    private final ProxyRankingProperties properties;

    // Immutable snapshot rebuilt from DB every 5 minutes.
    // Read path is lock-free; write path refreshes after upsert.
    private volatile Map<SegmentKey, SegmentStats> snapshot = Map.of();

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedRate = 300_000)
    public void refresh() {
        Map<SegmentKey, SegmentStats> fresh = new HashMap<>();
        repository.findAll().forEach(e -> fresh.put(
                new SegmentKey(e.getId().getProxyId(), e.getId().getCountry(), e.getId().getOs()),
                new SegmentStats(e.getWorkedCount(), e.getFailedCount(),
                        e.getLikelyWorkedCount(), e.getNextClickedCount())
        ));
        snapshot = Map.copyOf(fresh);
        log.debug("Segment stats snapshot refreshed: {} entries", fresh.size());
    }

    /**
     * Records WORKED/FAILED directly to proxy_segment_stats (immediate effect).
     * OPEN_TELEGRAM and NEXT_CLICKED are handled by the session aggregator.
     */
    @Transactional
    public void recordDirectSignal(Long proxyId, String country, String os, SignalEvent event) {
        long worked = event == SignalEvent.WORKED ? 1 : 0;
        long failed = event == SignalEvent.FAILED ? 1 : 0;
        repository.upsertSignal(proxyId, country, os, worked, failed);
    }

    /**
     * Returns a composite segment multiplier in [0, 1] for scoring.
     *
     * Two modes:
     * - Thompson sampling (useThompsonSampling=true): samples θ from Beta(α, β) where
     *   α = weightedSuccesses + 1, β = weightedFailures + 1. Unknown proxies get
     *   Beta(1,1) = Uniform(0,1) for natural exploration. NON-DETERMINISTIC.
     * - Wilson mode: returns the 95% CI lower bound deterministically.
     *
     * Callers must pre-compute multipliers ONCE per request to ensure sort stability.
     */
    public double getMultiplier(Long proxyId, ClientSegment segment) {
        ProxyRankingProperties.SegmentScoringProps cfg = properties.segmentScoring();
        if (!cfg.enabled()) return 1.0;

        SegmentKey key = new SegmentKey(proxyId, segment.country(), segment.os());
        SegmentStats stats = snapshot.get(key);

        long successes = stats == null ? 0
                : stats.workedCount() * cfg.workedWeight()
                + stats.likelyWorkedCount() * cfg.likelyWorkedWeight();
        long failures = stats == null ? 0
                : stats.failedCount() * cfg.failedWeight()
                + stats.nextClickedCount() * cfg.nextClickedWeight();

        if (cfg.useThompsonSampling()) {
            // Alpha = successes + 1, Beta = failures + 1 (uniform prior for no data)
            return BetaSampler.sample(successes + 1.0, failures + 1.0);
        }

        // Wilson mode: fall back to unknown multiplier if below min sample size
        long total = successes + failures;
        if (stats == null || total < cfg.minSampleSize()) return cfg.unknownProxyMultiplier();
        return WilsonScore.lowerBound(successes, total);
    }

    record SegmentKey(Long proxyId, String country, String os) {}

    record SegmentStats(long workedCount, long failedCount, long likelyWorkedCount, long nextClickedCount) {}
}
