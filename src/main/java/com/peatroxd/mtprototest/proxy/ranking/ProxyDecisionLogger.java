package com.peatroxd.mtprototest.proxy.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyDecisionLogger {

    private static final Logger DECISION_LOG = LoggerFactory.getLogger("proxy.selection.decisions");

    private final ProxyRankingProperties properties;
    private final ObjectMapper objectMapper;

    public void log(DecisionContext ctx) {
        if (!properties.decisionLogging().enabled()) return;
        try {
            DECISION_LOG.info(objectMapper.writeValueAsString(ctx));
        } catch (Exception e) {
            log.warn("Failed to serialize proxy selection decision", e);
        }
    }

    public record DecisionContext(
            String clientKey,
            ClientSegment segment,
            int candidatesTotal,
            int candidatesAfterBlacklist,
            int candidatesAfterOverrides,
            Long selectedProxyId,
            String selectionReason,
            List<Long> blacklistedIds,
            List<Long> excludedByOverrides,
            List<Long> demotedByOverrides,
            boolean segmentScoringActive
    ) {}
}
