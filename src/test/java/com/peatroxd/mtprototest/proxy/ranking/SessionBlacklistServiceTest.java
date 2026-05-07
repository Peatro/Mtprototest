package com.peatroxd.mtprototest.proxy.ranking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionBlacklistServiceTest {

    private SessionBlacklistService service;

    @BeforeEach
    void setUp() {
        ProxyRankingProperties props = new ProxyRankingProperties(
                new ProxyRankingProperties.SessionBlacklistProps(true, 30, 100, 5),
                new ProxyRankingProperties.SegmentOverridesProps(false, List.of()),
                new ProxyRankingProperties.SegmentScoringProps(false, 10, 1.0, 2, 3, 2, 1),
                new ProxyRankingProperties.SessionAggregationProps(false, 15),
                new ProxyRankingProperties.DecisionLoggingProps(false)
        );
        service = new SessionBlacklistService(props);
    }

    @Test
    void newClientHasEmptyHistory() {
        Set<Long> ids = service.getShownProxyIds("client-new");
        assertThat(ids).isEmpty();
    }

    @Test
    void recordedProxyAppearsInHistory() {
        service.recordShown("client-a", List.of(42L, 99L));
        assertThat(service.getShownProxyIds("client-a")).containsExactlyInAnyOrder(42L, 99L);
    }

    @Test
    void differentClientsHaveSeparateHistories() {
        service.recordShown("client-a", List.of(1L));
        service.recordShown("client-b", List.of(2L));

        assertThat(service.getShownProxyIds("client-a")).containsOnly(1L);
        assertThat(service.getShownProxyIds("client-b")).containsOnly(2L);
    }

    @Test
    void maxHistoryLimitIsRespected() {
        ProxyRankingProperties smallProps = new ProxyRankingProperties(
                new ProxyRankingProperties.SessionBlacklistProps(true, 30, 3, 5),
                new ProxyRankingProperties.SegmentOverridesProps(false, List.of()),
                new ProxyRankingProperties.SegmentScoringProps(false, 10, 1.0, 2, 3, 2, 1),
                new ProxyRankingProperties.SessionAggregationProps(false, 15),
                new ProxyRankingProperties.DecisionLoggingProps(false)
        );
        SessionBlacklistService small = new SessionBlacklistService(smallProps);

        small.recordShown("client", List.of(1L, 2L, 3L, 4L, 5L));

        assertThat(small.getShownProxyIds("client")).hasSize(3);
    }

    @Test
    void disabledBlacklistAlwaysReturnsEmpty() {
        ProxyRankingProperties disabled = new ProxyRankingProperties(
                new ProxyRankingProperties.SessionBlacklistProps(false, 30, 100, 5),
                new ProxyRankingProperties.SegmentOverridesProps(false, List.of()),
                new ProxyRankingProperties.SegmentScoringProps(false, 10, 1.0, 2, 3, 2, 1),
                new ProxyRankingProperties.SessionAggregationProps(false, 15),
                new ProxyRankingProperties.DecisionLoggingProps(false)
        );
        SessionBlacklistService svc = new SessionBlacklistService(disabled);
        svc.recordShown("client", List.of(1L, 2L));

        assertThat(svc.getShownProxyIds("client")).isEmpty();
    }
}
