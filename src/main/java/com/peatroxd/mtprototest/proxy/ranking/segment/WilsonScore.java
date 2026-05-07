package com.peatroxd.mtprototest.proxy.ranking.segment;

/**
 * Wilson score interval lower bound, used as a conservative success-rate estimator
 * that naturally shrinks toward 0 for small samples.
 */
public final class WilsonScore {

    private static final double Z = 1.96; // 95% confidence
    private static final double Z2 = Z * Z;

    private WilsonScore() {}

    /**
     * Returns the lower bound of the Wilson 95% CI for a Bernoulli proportion.
     *
     * @param successes number of positive outcomes (worked signals)
     * @param total     total trials (worked + failed)
     * @return lower bound in [0, 1]; returns 0 if total == 0
     */
    public static double lowerBound(long successes, long total) {
        if (total <= 0) return 0.0;
        double n = total;
        double p = (double) successes / n;
        double center = p + Z2 / (2 * n);
        double margin = Z * Math.sqrt(p * (1 - p) / n + Z2 / (4 * n * n));
        double denominator = 1 + Z2 / n;
        double lower = (center - margin) / denominator;
        return Math.max(0.0, Math.min(1.0, lower));
    }
}
