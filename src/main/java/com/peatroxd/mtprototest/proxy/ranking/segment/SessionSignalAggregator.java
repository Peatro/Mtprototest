package com.peatroxd.mtprototest.proxy.ranking.segment;

import com.peatroxd.mtprototest.proxy.ranking.ProxyRankingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes completed proxy sessions to derive implicit signals:
 * - likely_worked: session had OPEN_TELEGRAM for a proxy and no FAILED/NEXT_CLICKED after it
 * - next_clicked:  session had NEXT_CLICKED for a proxy (explicit rejection)
 *
 * Runs every 2 minutes; processes sessions inactive for longer than sessionTimeoutMinutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionSignalAggregator {

    private final ProxySessionSignalRepository signalRepository;
    private final ProxySegmentStatsRepository statsRepository;
    private final ProxySegmentStatsService segmentStatsService;
    private final ProxyRankingProperties properties;

    @Scheduled(fixedRate = 120_000)
    @Transactional
    public void aggregate() {
        ProxyRankingProperties.SessionAggregationProps cfg = properties.sessionAggregation();
        if (!cfg.enabled()) return;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cfg.sessionTimeoutMinutes());
        List<String> completedSessions = signalRepository.findCompletedSessionIds(cutoff);
        if (completedSessions.isEmpty()) return;

        List<ProxySessionSignalEntity> signals = signalRepository.findAllBySessionIdIn(completedSessions);
        Map<SessionProxyKey, List<ProxySessionSignalEntity>> grouped = groupBySessionProxy(signals);

        int likelyWorkedTotal = 0;
        int nextClickedTotal = 0;

        for (Map.Entry<SessionProxyKey, List<ProxySessionSignalEntity>> entry : grouped.entrySet()) {
            SessionProxyKey key = entry.getKey();
            List<ProxySessionSignalEntity> events = entry.getValue();
            events.sort((a, b) -> a.getOccurredAt().compareTo(b.getOccurredAt()));

            AggregatedSignal agg = computeSignals(events);
            if (agg.likelyWorked() > 0 || agg.nextClicked() > 0) {
                statsRepository.upsertComposite(
                        key.proxyId(), key.country(), key.os(),
                        agg.likelyWorked(), agg.nextClicked()
                );
                likelyWorkedTotal += agg.likelyWorked();
                nextClickedTotal += agg.nextClicked();
            }
        }

        signalRepository.deleteBySessionIdIn(completedSessions);
        segmentStatsService.refresh();

        log.debug("Session aggregator: processed {} sessions, likely_worked={}, next_clicked={}",
                completedSessions.size(), likelyWorkedTotal, nextClickedTotal);
    }

    /**
     * Determines likely_worked and next_clicked signals from an ordered event list
     * for a single (session, proxy) pair.
     *
     * likely_worked = OPEN_TELEGRAM exists AND no FAILED/NEXT_CLICKED comes after it
     * next_clicked  = at least one NEXT_CLICKED event exists
     */
    AggregatedSignal computeSignals(List<ProxySessionSignalEntity> events) {
        boolean hasOpenTelegram = false;
        boolean negativeAfterOpen = false;
        boolean hasNextClicked = false;

        for (ProxySessionSignalEntity e : events) {
            switch (e.getEvent()) {
                case OPEN_TELEGRAM -> hasOpenTelegram = true;
                case NEXT_CLICKED -> {
                    hasNextClicked = true;
                    if (hasOpenTelegram) negativeAfterOpen = true;
                }
                case FAILED -> {
                    if (hasOpenTelegram) negativeAfterOpen = true;
                }
                case WORKED -> {
                    // explicit positive — already counted in worked_count via direct upsert
                }
            }
        }

        long likelyWorked = (hasOpenTelegram && !negativeAfterOpen) ? 1 : 0;
        long nextClicked  = hasNextClicked ? 1 : 0;
        return new AggregatedSignal(likelyWorked, nextClicked);
    }

    private Map<SessionProxyKey, List<ProxySessionSignalEntity>> groupBySessionProxy(
            List<ProxySessionSignalEntity> signals) {
        Map<SessionProxyKey, List<ProxySessionSignalEntity>> map = new HashMap<>();
        for (ProxySessionSignalEntity s : signals) {
            // derive key using country + os from first event; all events for same session+proxy share them
            SessionProxyKey key = new SessionProxyKey(s.getSessionId(), s.getProxyId(), s.getCountry(), s.getOs());
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return map;
    }

    record SessionProxyKey(String sessionId, Long proxyId, String country, String os) {}
    record AggregatedSignal(long likelyWorked, long nextClicked) {}
}
