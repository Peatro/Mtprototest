package com.peatroxd.mtprototest.common.health;

import com.peatroxd.mtprototest.admin.service.ProxyImportTrackingService;
import com.peatroxd.mtprototest.admin.service.SourceImportSnapshot;
import com.peatroxd.mtprototest.parser.config.ParserSourcesProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyImportHealthIndicatorTest {

    @Test
    void shouldReportDownWhenEnabledSourceHasNoSuccessfulSnapshot() {
        ParserSourcesProperties properties = new ParserSourcesProperties();
        ParserSourcesProperties.SourceDefinition source = new ParserSourcesProperties.SourceDefinition();
        source.setName("source_a");
        source.setEnabled(true);
        properties.setEntries(List.of(source));

        ProxyImportTrackingService trackingService = mock(ProxyImportTrackingService.class);
        when(trackingService.getSnapshots()).thenReturn(List.of(
                new SourceImportSnapshot("source_a", LocalDateTime.now().minusMinutes(2), LocalDateTime.now().minusMinutes(1), false, 0, 0, 0, "boom")
        ));

        var health = new ProxyImportHealthIndicator(properties, trackingService).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("failedSources", List.of("source_a"));
    }
}
