package com.peatroxd.mtprototest.proxy.ranking;

import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.ranking.segment.ProxySegmentStatsService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankedProxySelector {

    private final ProxyService proxyService;
    private final SessionBlacklistService sessionBlacklist;
    private final GeoIpResolver geoIpResolver;
    private final UserAgentParser userAgentParser;
    private final ProxyDecisionLogger decisionLogger;
    private final ProxyRankingProperties properties;
    private final ProxySegmentStatsService segmentStatsService;

    public List<ProxyResponse> getBestForClient(HttpServletRequest request, String clientKey) {
        List<ProxyResponse> candidates = proxyService.getBest();
        ClientSegment segment = resolveSegment(request);

        Set<Long> blacklisted = getBlacklistedSafely(clientKey);
        List<ProxyResponse> afterBlacklist = applySessionBlacklist(candidates, blacklisted);

        List<Long> excludedByOverrides = new ArrayList<>();
        List<Long> demotedByOverrides = new ArrayList<>();
        List<ProxyResponse> afterOverrides = applySegmentOverrides(
                afterBlacklist, segment, excludedByOverrides, demotedByOverrides);

        List<ProxyResponse> afterScoring = applySegmentScoring(afterOverrides, segment);

        Long selectedId = afterScoring.isEmpty() ? null : afterScoring.get(0).id();
        String reason = resolveReason(candidates, afterBlacklist, afterOverrides);

        decisionLogger.log(new ProxyDecisionLogger.DecisionContext(
                clientKey,
                segment,
                candidates.size(),
                afterBlacklist.size(),
                afterOverrides.size(),
                selectedId,
                reason,
                new ArrayList<>(blacklisted),
                excludedByOverrides,
                demotedByOverrides,
                properties.segmentScoring().enabled()
        ));

        recordShownSafely(clientKey, afterScoring);

        return afterScoring;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private ClientSegment resolveSegment(HttpServletRequest request) {
        String country = geoIpResolver.resolve(request);
        String ua = request.getHeader("User-Agent");
        return new ClientSegment(country, userAgentParser.parseOs(ua), userAgentParser.parseDeviceType(ua));
    }

    private Set<Long> getBlacklistedSafely(String clientKey) {
        try {
            return sessionBlacklist.getShownProxyIds(clientKey);
        } catch (Exception e) {
            log.warn("Session blacklist read failed, continuing without it", e);
            return Set.of();
        }
    }

    private List<ProxyResponse> applySessionBlacklist(List<ProxyResponse> candidates, Set<Long> blacklisted) {
        if (!properties.sessionBlacklist().enabled() || blacklisted.isEmpty()) return candidates;
        List<ProxyResponse> filtered = candidates.stream()
                .filter(p -> !blacklisted.contains(p.id()))
                .toList();
        // Graceful degradation: if all candidates are blacklisted, return original list
        return filtered.isEmpty() ? candidates : filtered;
    }

    private List<ProxyResponse> applySegmentOverrides(
            List<ProxyResponse> candidates,
            ClientSegment segment,
            List<Long> excludedOut,
            List<Long> demotedOut) {

        if (!properties.segmentOverrides().enabled()) return candidates;

        List<ProxyRankingProperties.SegmentOverridesProps.OverrideRule> rules =
                properties.segmentOverrides().rules();
        if (rules == null || rules.isEmpty()) return candidates;

        Map<Long, Double> multipliers = rules.stream()
                .filter(r -> segment.matchesCountryAndOs(r.country(), r.os()))
                .collect(Collectors.toMap(
                        ProxyRankingProperties.SegmentOverridesProps.OverrideRule::proxyId,
                        ProxyRankingProperties.SegmentOverridesProps.OverrideRule::multiplier,
                        (a, b) -> b
                ));

        if (multipliers.isEmpty()) return candidates;

        List<ProxyResponse> normal = new ArrayList<>();
        List<ProxyResponse> demoted = new ArrayList<>();

        for (ProxyResponse proxy : candidates) {
            double m = multipliers.getOrDefault(proxy.id(), 1.0);
            if (m <= 0.0) {
                excludedOut.add(proxy.id());
            } else if (m < 1.0) {
                demotedOut.add(proxy.id());
                demoted.add(proxy);
            } else {
                normal.add(proxy);
            }
        }

        // Normal proxies first, demoted at the end; excluded are dropped
        List<ProxyResponse> result = new ArrayList<>(normal);
        result.addAll(demoted);

        // Graceful degradation: if everything was excluded, return original
        return result.isEmpty() ? candidates : result;
    }

    private void recordShownSafely(String clientKey, List<ProxyResponse> shown) {
        try {
            List<Long> topIds = shown.stream()
                    .limit(properties.sessionBlacklist().recordTopN())
                    .map(ProxyResponse::id)
                    .toList();
            sessionBlacklist.recordShown(clientKey, topIds);
        } catch (Exception e) {
            log.warn("Session blacklist write failed, continuing", e);
        }
    }

    private List<ProxyResponse> applySegmentScoring(List<ProxyResponse> candidates, ClientSegment segment) {
        if (!properties.segmentScoring().enabled()) return candidates;
        return candidates.stream()
                .sorted(Comparator.comparingDouble((ProxyResponse p) ->
                        p.score() * segmentStatsService.getMultiplier(p.id(), segment)
                ).reversed())
                .toList();
    }

    private String resolveReason(
            List<ProxyResponse> original,
            List<ProxyResponse> afterBlacklist,
            List<ProxyResponse> afterOverrides) {

        if (afterOverrides.isEmpty()) return "fallback_all_filtered";
        if (afterBlacklist.size() < original.size() && afterOverrides.size() < afterBlacklist.size()) {
            return "blacklist_and_overrides_applied";
        }
        if (afterBlacklist.size() < original.size()) return "blacklist_applied";
        if (afterOverrides.size() < afterBlacklist.size()) return "overrides_applied";
        return "no_filter_applied";
    }
}
