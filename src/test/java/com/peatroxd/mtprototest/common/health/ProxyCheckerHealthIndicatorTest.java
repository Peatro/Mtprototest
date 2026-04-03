package com.peatroxd.mtprototest.common.health;

import com.peatroxd.mtprototest.checker.config.CheckerProperties;
import com.peatroxd.mtprototest.checker.service.CheckCycleSnapshot;
import com.peatroxd.mtprototest.checker.service.ProxyCheckCycleTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyCheckerHealthIndicatorTest {

    @Test
    void shouldReportUpWhenRecentSuccessfulCycleExists() {
        ProxyCheckCycleTrackingService trackingService = mock(ProxyCheckCycleTrackingService.class);
        CheckerProperties checkerProperties = new CheckerProperties();
        checkerProperties.setFixedDelayMs(300_000);

        CheckCycleSnapshot snapshot = new CheckCycleSnapshot(
                "scheduled",
                LocalDateTime.now().minusSeconds(20),
                LocalDateTime.now().minusSeconds(5),
                true,
                120,
                3,
                false,
                null,
                Duration.ofSeconds(15)
        );

        when(trackingService.latestCycle()).thenReturn(Optional.of(snapshot));
        when(trackingService.latestSuccessfulCycle()).thenReturn(Optional.of(snapshot));

        var health = new ProxyCheckerHealthIndicator(trackingService, checkerProperties).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("latestTrigger", "scheduled");
    }
}
