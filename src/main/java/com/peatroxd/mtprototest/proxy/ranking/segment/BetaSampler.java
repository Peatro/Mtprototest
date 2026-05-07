package com.peatroxd.mtprototest.proxy.ranking.segment;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Samples from a Beta(alpha, beta) distribution using the ratio-of-Gammas method.
 * Gamma samples are generated via the Marsaglia-Tsang squeeze algorithm (2000),
 * which works for any shape parameter >= 1 and is adapted for alpha < 1 via
 * the standard Gamma(alpha) = Gamma(alpha+1) * U^(1/alpha) identity.
 *
 * This implementation is zero-dependency and thread-safe (uses ThreadLocalRandom).
 */
public final class BetaSampler {

    private BetaSampler() {}

    /**
     * Returns one sample from Beta(alpha, beta) in [0, 1].
     * Both parameters must be > 0.
     */
    public static double sample(double alpha, double beta) {
        double x = sampleGamma(alpha);
        double y = sampleGamma(beta);
        double sum = x + y;
        if (sum == 0) return 0.5; // degenerate guard
        return x / sum;
    }

    private static double sampleGamma(double alpha) {
        if (alpha < 1.0) {
            // Gamma(alpha) = Gamma(alpha+1) * U^(1/alpha)
            return sampleGamma(alpha + 1.0)
                    * Math.pow(ThreadLocalRandom.current().nextDouble(), 1.0 / alpha);
        }
        return marsagliaTsang(alpha);
    }

    private static double marsagliaTsang(double alpha) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double d = alpha - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x, v;
            do {
                x = rng.nextGaussian();
                v = 1.0 + c * x;
            } while (v <= 0.0);
            v = v * v * v;
            double u = rng.nextDouble();
            double x2 = x * x;
            if (u < 1.0 - 0.0331 * x2 * x2) return d * v;
            if (Math.log(u) < 0.5 * x2 + d * (1.0 - v + Math.log(v))) return d * v;
        }
    }
}
