package com.peatroxd.mtprototest.proxy.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.ranking.segment.ProxySegmentStatsService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankedProxySelectorTest {

    @Mock private ProxyService proxyService;
    @Mock private HttpServletRequest request;
    @Mock private ProxySegmentStatsService segmentStatsService;

    private RankedProxySelector selector;

    private static final List<Long> EXCLUDED_IDS = List.of(678L, 776L, 883L, 1033L);
    private static final List<Long> DEMOTED_IDS  = List.of(688L, 879L);

    @BeforeEach
    void setUp() {
        ProxyRankingProperties props = new ProxyRankingProperties(
                new ProxyRankingProperties.SessionBlacklistProps(true, 30, 100, 1),
                new ProxyRankingProperties.SegmentOverridesProps(true, List.of(
                        new ProxyRankingProperties.SegmentOverridesProps.OverrideRule("RU", "Windows", 678L,  0.0),
                        new ProxyRankingProperties.SegmentOverridesProps.OverrideRule("RU", "Windows", 776L,  0.0),
                        new ProxyRankingProperties.SegmentOverridesProps.OverrideRule("RU", "Windows", 883L,  0.0),
                        new ProxyRankingProperties.SegmentOverridesProps.OverrideRule("RU", "Windows", 1033L, 0.0),
                        new ProxyRankingProperties.SegmentOverridesProps.OverrideRule("RU", "Windows", 688L,  0.3),
                        new ProxyRankingProperties.SegmentOverridesProps.OverrideRule("RU", "Windows", 879L,  0.3)
                )),
                new ProxyRankingProperties.SegmentScoringProps(false, false, 10, 1.0, 2, 3, 2, 1),
                new ProxyRankingProperties.SessionAggregationProps(false, 15),
                new ProxyRankingProperties.DecisionLoggingProps(false)
        );

        SessionBlacklistService blacklist = new SessionBlacklistService(props);
        GeoIpResolver geoIp = new GeoIpResolver();
        UserAgentParser uaParser = new UserAgentParser();
        ProxyDecisionLogger logger = new ProxyDecisionLogger(props, new ObjectMapper());

        selector = new RankedProxySelector(proxyService, blacklist, geoIp, uaParser, logger, props, segmentStatsService);
    }

    // ── Segment overrides ──────────────────────────────────────────────────

    @Test
    void excludedProxiesNeverReturnedForRuWindows() {
        stubCandidates(1L, 678L, 776L, 883L, 1033L, 2L);
        stubRuWindowsRequest();

        List<Long> ids = getResultIds();

        assertThat(ids).doesNotContainAnyElementsOf(EXCLUDED_IDS);
        assertThat(ids).contains(1L, 2L);
    }

    @Test
    void demotedProxiesAppearsAfterNormalOnesForRuWindows() {
        stubCandidates(688L, 879L, 1L, 2L, 3L);
        stubRuWindowsRequest();

        List<Long> ids = getResultIds();

        int pos1  = ids.indexOf(1L);
        int pos688 = ids.indexOf(688L);
        assertThat(pos1).isLessThan(pos688);
    }

    @Test
    void overridesNotAppliedForOtherSegments() {
        stubCandidates(678L, 688L, 1L);
        when(request.getHeader("CF-IPCountry")).thenReturn("DE");
        when(request.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        List<Long> ids = getResultIds();

        assertThat(ids).contains(678L, 688L);
    }

    // ── Session blacklist ──────────────────────────────────────────────────

    @Test
    void sameProxyNotReturnedAsFirstInSubsequentCall() {
        stubCandidates(10L, 20L, 30L);
        stubRuWindowsRequest();

        // First call — 10L is recorded as shown
        List<Long> first = getResultIds();
        assertThat(first.get(0)).isEqualTo(10L);

        // Second call — 10L is blacklisted, 20L should be first
        List<Long> second = getResultIds();
        assertThat(second.get(0)).isEqualTo(20L);
        assertThat(second).doesNotContain(10L);
    }

    @Test
    void gracefulDegradationWhenAllCandidatesBlacklisted() {
        stubCandidates(5L);
        stubRuWindowsRequest();

        getResultIds(); // records 5L as shown
        List<Long> second = getResultIds(); // 5L blacklisted but only candidate

        assertThat(second).containsExactly(5L); // falls back to original list
    }

    @Test
    void gracefulDegradationWhenAllExcludedByOverrides() {
        // Only excluded proxies — should fall back to original list
        stubCandidates(678L, 776L);
        stubRuWindowsRequest();

        List<Long> ids = getResultIds();

        assertThat(ids).containsExactlyInAnyOrder(678L, 776L);
    }

    // ── Segment scoring ────────────────────────────────────────────────────

    @Test
    void segmentScoringReordersProxiesWhenEnabled() {
        // Build a selector with scoring enabled
        ProxyRankingProperties scoringProps = new ProxyRankingProperties(
                new ProxyRankingProperties.SessionBlacklistProps(false, 30, 100, 5),
                new ProxyRankingProperties.SegmentOverridesProps(false, List.of()),
                new ProxyRankingProperties.SegmentScoringProps(true, false, 10, 1.0, 2, 3, 2, 1),
                new ProxyRankingProperties.SessionAggregationProps(false, 15),
                new ProxyRankingProperties.DecisionLoggingProps(false)
        );
        ProxyDecisionLogger logger = new ProxyDecisionLogger(scoringProps, new ObjectMapper());
        RankedProxySelector scoringSelector = new RankedProxySelector(
                proxyService,
                new SessionBlacklistService(scoringProps),
                new GeoIpResolver(),
                new UserAgentParser(),
                logger,
                scoringProps,
                segmentStatsService
        );

        // Proxy 10 has score 80, proxy 20 has score 80 — equal base scores
        List<ProxyResponse> proxies = List.of(
                ProxyResponse.builder().id(10L).host("h10").port(443).secret("s").type("MTPROTO")
                        .source("t").status("ALIVE").verificationStatus("VERIFIED").score(80)
                        .consecutiveFailures(0).consecutiveSuccesses(1).verified(true).build(),
                ProxyResponse.builder().id(20L).host("h20").port(443).secret("s").type("MTPROTO")
                        .source("t").status("ALIVE").verificationStatus("VERIFIED").score(80)
                        .consecutiveFailures(0).consecutiveSuccesses(1).verified(true).build()
        );
        when(proxyService.getBest()).thenReturn(proxies);
        when(request.getHeader("CF-IPCountry")).thenReturn("DE");
        when(request.getHeader("User-Agent")).thenReturn(WINDOWS_UA);

        // Proxy 20 is known-good (multiplier 0.9), proxy 10 is unknown (neutral 1.0) — 10 stays first
        when(segmentStatsService.getMultiplier(eq(10L), any())).thenReturn(1.0);
        when(segmentStatsService.getMultiplier(eq(20L), any())).thenReturn(0.9);

        List<Long> ids = scoringSelector.getBestForClient(request, "c")
                .stream().map(ProxyResponse::id).toList();

        assertThat(ids.get(0)).isEqualTo(10L);
        assertThat(ids.get(1)).isEqualTo(20L);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static final String WINDOWS_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private void stubRuWindowsRequest() {
        when(request.getHeader("CF-IPCountry")).thenReturn("RU");
        when(request.getHeader("User-Agent")).thenReturn(WINDOWS_UA);
    }

    private void stubCandidates(Long... ids) {
        List<ProxyResponse> proxies = LongStream.of(
                        java.util.Arrays.stream(ids).mapToLong(Long::longValue).toArray())
                .mapToObj(id -> ProxyResponse.builder().id(id).host("h" + id).port(443)
                        .secret("secret").type("MTPROTO").source("test")
                        .status("ALIVE").verificationStatus("VERIFIED").score(80)
                        .consecutiveFailures(0).consecutiveSuccesses(1).verified(true).build())
                .toList();
        when(proxyService.getBest()).thenReturn(proxies);
    }

    private List<Long> getResultIds() {
        return selector.getBestForClient(request, "test-client")
                .stream().map(ProxyResponse::id).toList();
    }
}
