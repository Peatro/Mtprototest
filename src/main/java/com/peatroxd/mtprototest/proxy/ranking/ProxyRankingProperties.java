package com.peatroxd.mtprototest.proxy.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "proxy.ranking")
public record ProxyRankingProperties(
        SessionBlacklistProps sessionBlacklist,
        SegmentOverridesProps segmentOverrides,
        SegmentScoringProps segmentScoring,
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

    public record SegmentScoringProps(
            boolean enabled,
            int minSampleSize,
            double unknownProxyMultiplier
    ) {
        public SegmentScoringProps {
            if (minSampleSize <= 0) minSampleSize = 10;
            if (unknownProxyMultiplier < 0 || unknownProxyMultiplier > 1) unknownProxyMultiplier = 1.0;
        }
    }

    public record DecisionLoggingProps(boolean enabled) {}
}
