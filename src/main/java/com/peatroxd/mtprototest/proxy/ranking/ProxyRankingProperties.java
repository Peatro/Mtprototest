package com.peatroxd.mtprototest.proxy.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "proxy.ranking")
public record ProxyRankingProperties(
        SessionBlacklistProps sessionBlacklist,
        SegmentOverridesProps segmentOverrides,
        SegmentScoringProps segmentScoring,
        SessionAggregationProps sessionAggregation,
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
            boolean useThompsonSampling,
            int minSampleSize,
            double unknownProxyMultiplier,
            int workedWeight,
            int likelyWorkedWeight,
            int failedWeight,
            int nextClickedWeight
    ) {
        public SegmentScoringProps {
            if (minSampleSize <= 0) minSampleSize = 10;
            if (unknownProxyMultiplier < 0 || unknownProxyMultiplier > 1) unknownProxyMultiplier = 1.0;
            if (workedWeight <= 0) workedWeight = 2;
            if (likelyWorkedWeight <= 0) likelyWorkedWeight = 3;
            if (failedWeight <= 0) failedWeight = 2;
            if (nextClickedWeight <= 0) nextClickedWeight = 1;
        }
    }

    public record SessionAggregationProps(
            boolean enabled,
            int sessionTimeoutMinutes
    ) {
        public SessionAggregationProps {
            if (sessionTimeoutMinutes <= 0) sessionTimeoutMinutes = 15;
        }
    }

    public record DecisionLoggingProps(boolean enabled) {}
}
