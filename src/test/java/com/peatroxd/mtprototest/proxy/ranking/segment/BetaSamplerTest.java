package com.peatroxd.mtprototest.proxy.ranking.segment;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BetaSamplerTest {

    private static final int SAMPLES = 50_000;

    // ── Output range ───────────────────────────────────────────────────────

    @RepeatedTest(10)
    void outputIsAlwaysInUnitInterval() {
        double v = BetaSampler.sample(1.0, 1.0);
        assertThat(v).isBetween(0.0, 1.0);
    }

    // ── Empirical means (theoretical: alpha / (alpha + beta)) ─────────────

    @Test
    void uniformPrior_meanNearHalf() {
        // Beta(1,1) = Uniform(0,1), mean = 0.5
        assertMeanNear(1.0, 1.0, 0.5, 0.01);
    }

    @Test
    void symmetricBeta_meanNearHalf() {
        // Beta(10,10), mean = 0.5
        assertMeanNear(10.0, 10.0, 0.5, 0.01);
    }

    @Test
    void highSuccessRate_meanNearOne() {
        // Beta(9,1), mean = 9/10 = 0.9
        assertMeanNear(9.0, 1.0, 0.9, 0.01);
    }

    @Test
    void lowSuccessRate_meanNearZero() {
        // Beta(1,9), mean = 1/10 = 0.1
        assertMeanNear(1.0, 9.0, 0.1, 0.01);
    }

    @Test
    void largeAlpha_meanNearOne() {
        // Beta(100,10), mean = 100/110 ≈ 0.909
        assertMeanNear(100.0, 10.0, 100.0 / 110.0, 0.005);
    }

    @Test
    void fractionalAlpha_producesValidOutputs() {
        // alpha < 1 uses the Gamma(alpha+1)*U^(1/alpha) branch
        assertMeanNear(0.5, 0.5, 0.5, 0.02);
    }

    // ── Exploration property ───────────────────────────────────────────────

    @Test
    void unknownProxy_beta11_producesHighVariance() {
        // Beta(1,1) = Uniform: std dev should be ~0.289 (1/sqrt(12))
        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) samples[i] = BetaSampler.sample(1.0, 1.0);
        double mean = mean(samples);
        double variance = variance(samples, mean);
        assertThat(Math.sqrt(variance)).isBetween(0.27, 0.31);
    }

    @Test
    void wellKnownGoodProxy_highAlpha_producesLowVariance() {
        // Beta(500,50): very confident it's good, std dev should be small
        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) samples[i] = BetaSampler.sample(500.0, 50.0);
        double mean = mean(samples);
        double stdDev = Math.sqrt(variance(samples, mean));
        assertThat(stdDev).isLessThan(0.02);
        assertThat(mean).isBetween(0.89, 0.92); // 500/550 ≈ 0.909
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void assertMeanNear(double alpha, double beta, double expectedMean, double tolerance) {
        double sum = 0;
        for (int i = 0; i < SAMPLES; i++) sum += BetaSampler.sample(alpha, beta);
        double empiricalMean = sum / SAMPLES;
        assertThat(empiricalMean)
                .as("Beta(%s,%s) empirical mean", alpha, beta)
                .isBetween(expectedMean - tolerance, expectedMean + tolerance);
    }

    private double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double variance(double[] values, double mean) {
        double sum = 0;
        for (double v : values) sum += (v - mean) * (v - mean);
        return sum / values.length;
    }
}
