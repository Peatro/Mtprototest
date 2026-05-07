package com.peatroxd.mtprototest.proxy.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "proxy.ranking")
public record ProxyRankingProperties(
        SessionBlacklistProps sessionBlacklist,
        SegmentOverridesProps segmentOverrides,
        DecisionLoggingProps decisionLogging
) {

    public record SessionBlacklistProps(
            boolean enabled,
            int windowMinutes,
            int maxHistoryPerSession,
            int recordTopN
    ) {
        public SessionBlacklistProps {
            if (windowMinutes <= 0) windowMinutes = 30;
            if (maxHistoryPerSession <= 0) maxHistoryPerSession = 100;
            if (recordTopN <= 0) recordTopN = 5;
        }
    }

    public record SegmentOverridesProps(
            boolean enabled,
            List<OverrideRule> rules
    ) {
        public record OverrideRule(String country, String os, Long proxyId, double multiplier) {}
    }

    public record DecisionLoggingProps(boolean enabled) {}
}
