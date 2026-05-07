package com.peatroxd.mtprototest.proxy.ranking.segment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WilsonScoreTest {

    @Test
    void zeroTotalReturnsZero() {
        assertThat(WilsonScore.lowerBound(0, 0)).isEqualTo(0.0);
    }

    @Test
    void perfectSuccessRateLargeNApproachesOne() {
        double lb = WilsonScore.lowerBound(1000, 1000);
        assertThat(lb).isGreaterThan(0.99);
    }

    @Test
    void zeroSuccessRateLargeNApproachesZero() {
        double lb = WilsonScore.lowerBound(0, 1000);
        assertThat(lb).isEqualTo(0.0);
    }

    @Test
    void smallSampleIsConservative() {
        // 10/10 worked but only 10 samples — lower bound should be noticeably < 1
        double lb = WilsonScore.lowerBound(10, 10);
        assertThat(lb).isLessThan(0.85);
    }

    @ParameterizedTest
    @CsvSource({
            "50,  100, 0.40, 0.60",  // 50% rate — LB somewhere in 40-60%
            "90,  100, 0.82, 0.92",  // 90% rate — LB should be high
            "10,  100, 0.05, 0.18",  // 10% rate — LB should be low
    })
    void lowerBoundIsWithinReasonableRange(long worked, long total, double min, double max) {
        double lb = WilsonScore.lowerBound(worked, total);
        assertThat(lb).isBetween(min, max);
    }

    @Test
    void resultIsAlwaysBetweenZeroAndOne() {
        // Boundary stress: all worked, none worked, mixed
        assertThat(WilsonScore.lowerBound(0, 1)).isBetween(0.0, 1.0);
        assertThat(WilsonScore.lowerBound(1, 1)).isBetween(0.0, 1.0);
        assertThat(WilsonScore.lowerBound(500, 1000)).isBetween(0.0, 1.0);
    }
}
