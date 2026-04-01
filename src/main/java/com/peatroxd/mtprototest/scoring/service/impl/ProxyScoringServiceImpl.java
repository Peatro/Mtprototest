package com.peatroxd.mtprototest.scoring.service.impl;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import org.springframework.stereotype.Service;

@Service
public class ProxyScoringServiceImpl implements ProxyScoringService {

    @Override
    public int calculateScore(ProxyStatus status, ProxyVerificationStatus verificationStatus, long latencyMs) {
        if (status != ProxyStatus.ALIVE) {
            return 0;
        }

        int baseScore = switch (verificationStatus) {
            case VERIFIED -> 100;
            case QUICK_OK -> 60;
            case UNVERIFIED -> 0;
        };

        if (baseScore == 0) {
            return 0;
        }

        int latencyPenalty = latencyMs > 0 ? (int) Math.min(30, latencyMs / 20) : 0;
        return Math.max(1, baseScore - latencyPenalty);
    }
}
