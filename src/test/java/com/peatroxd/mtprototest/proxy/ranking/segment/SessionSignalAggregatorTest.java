package com.peatroxd.mtprototest.proxy.ranking.segment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionSignalAggregatorTest {

    private SessionSignalAggregator aggregator;

    @BeforeEach
    void setUp() {
        // Aggregator only needs its own logic — no repos needed for unit tests
        aggregator = new SessionSignalAggregator(null, null, null, null);
    }

    // ── computeSignals ─────────────────────────────────────────────────────

    @Test
    void likelyWorked_whenOpenTelegramAndNoNegativeFollow() {
        var signals = events(
                event(SignalEvent.OPEN_TELEGRAM, 0),
                event(SignalEvent.WORKED, 5) // worked doesn't count as negative follow
        );
        var result = aggregator.computeSignals(signals);

        assertThat(result.likelyWorked()).isEqualTo(1);
        assertThat(result.nextClicked()).isEqualTo(0);
    }

    @Test
    void notLikelyWorked_whenNextClickedAfterOpenTelegram() {
        var signals = events(
                event(SignalEvent.OPEN_TELEGRAM, 0),
                event(SignalEvent.NEXT_CLICKED, 3)
        );
        var result = aggregator.computeSignals(signals);

        assertThat(result.likelyWorked()).isEqualTo(0);
        assertThat(result.nextClicked()).isEqualTo(1);
    }

    @Test
    void notLikelyWorked_whenFailedAfterOpenTelegram() {
        var signals = events(
                event(SignalEvent.OPEN_TELEGRAM, 0),
                event(SignalEvent.FAILED, 2)
        );
        var result = aggregator.computeSignals(signals);

        assertThat(result.likelyWorked()).isEqualTo(0);
    }

    @Test
    void notLikelyWorked_whenNoOpenTelegram() {
        var signals = events(event(SignalEvent.WORKED, 0));
        var result = aggregator.computeSignals(signals);

        assertThat(result.likelyWorked()).isEqualTo(0);
    }

    @Test
    void nextClickedWithoutOpenTelegram_stillCountsAsNextClicked() {
        var signals = events(event(SignalEvent.NEXT_CLICKED, 0));
        var result = aggregator.computeSignals(signals);

        assertThat(result.nextClicked()).isEqualTo(1);
        assertThat(result.likelyWorked()).isEqualTo(0);
    }

    @Test
    void nextClickedBeforeOpenTelegram_doesNotInvalidateLikelyWorked() {
        // User clicked next for a different proxy, then opened telegram for this one
        var signals = events(
                event(SignalEvent.NEXT_CLICKED, 0),
                event(SignalEvent.OPEN_TELEGRAM, 5)
        );
        var result = aggregator.computeSignals(signals);

        assertThat(result.likelyWorked()).isEqualTo(1);
        assertThat(result.nextClicked()).isEqualTo(1);
    }

    @Test
    void emptyEventList_returnsZeroSignals() {
        var result = aggregator.computeSignals(List.of());

        assertThat(result.likelyWorked()).isEqualTo(0);
        assertThat(result.nextClicked()).isEqualTo(0);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private List<ProxySessionSignalEntity> events(ProxySessionSignalEntity... entities) {
        return List.of(entities);
    }

    private ProxySessionSignalEntity event(SignalEvent type, int secondsOffset) {
        return ProxySessionSignalEntity.builder()
                .sessionId("s1")
                .proxyId(42L)
                .event(type)
                .country("RU")
                .os("Windows")
                .occurredAt(LocalDateTime.now().plusSeconds(secondsOffset))
                .build();
    }
}
