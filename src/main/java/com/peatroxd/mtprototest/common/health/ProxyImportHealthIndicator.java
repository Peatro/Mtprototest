package com.peatroxd.mtprototest.common.health;

import com.peatroxd.mtprototest.admin.service.ProxyImportTrackingService;
import com.peatroxd.mtprototest.admin.service.SourceImportSnapshot;
import com.peatroxd.mtprototest.parser.config.ParserSourcesProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("proxyImports")
public class ProxyImportHealthIndicator implements HealthIndicator {

    private final ParserSourcesProperties parserSourcesProperties;
    private final ProxyImportTrackingService proxyImportTrackingService;

    public ProxyImportHealthIndicator(
            ParserSourcesProperties parserSourcesProperties,
            ProxyImportTrackingService proxyImportTrackingService
    ) {
        this.parserSourcesProperties = parserSourcesProperties;
        this.proxyImportTrackingService = proxyImportTrackingService;
    }

    @Override
    public Health health() {
        List<String> enabledSources = parserSourcesProperties.getEntries().stream()
                .filter(ParserSourcesProperties.SourceDefinition::isEnabled)
                .map(ParserSourcesProperties.SourceDefinition::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();

        Collection<SourceImportSnapshot> snapshots = proxyImportTrackingService.getSnapshots();
        Map<String, SourceImportSnapshot> bySource = snapshots.stream()
                .collect(Collectors.toMap(SourceImportSnapshot::source, snapshot -> snapshot, (left, right) -> right));

        List<String> missingSources = enabledSources.stream()
                .filter(source -> !bySource.containsKey(source))
                .toList();
        List<String> failedSources = enabledSources.stream()
                .map(bySource::get)
                .filter(snapshot -> snapshot != null && snapshot.completedAt() != null && !snapshot.succeeded())
                .map(SourceImportSnapshot::source)
                .toList();

        Health.Builder builder;
        if (enabledSources.isEmpty()) {
            builder = Health.unknown();
        } else if (!missingSources.isEmpty() || !failedSources.isEmpty()) {
            builder = Health.down();
        } else {
            builder = Health.up();
        }

        return builder
                .withDetail("enabledSources", enabledSources)
                .withDetail("trackedSources", snapshots.size())
                .withDetail("missingSources", missingSources)
                .withDetail("failedSources", failedSources)
                .build();
    }
}
