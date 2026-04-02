package com.peatroxd.mtprototest.scoring.service;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;

public interface ProxyScoringService {
    int calculateScore(ProxyStatus status, ProxyVerificationStatus verificationStatus, long latencyMs);
}
