package com.peatroxd.mtprototest.scoring.service.impl;

import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import org.springframework.stereotype.Service;

@Service
public class ProxyScoringServiceImpl implements ProxyScoringService {

    @Override
    public int calculateScore(boolean alive, long latencyMs) {
        if (!alive) {
            return 0;
        }

        int latencyPenalty = latencyMs > 0 ? (int) Math.min(99, latencyMs / 10) : 0;
        return Math.max(1, 100 - latencyPenalty);
    }
}
