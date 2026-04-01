package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.checker.service.ProxyCheckUpdateService;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import com.peatroxd.mtprototest.scoring.service.ProxyScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProxyCheckUpdateServiceImpl implements ProxyCheckUpdateService {

    private final ProxyRepository proxyRepository;
    private final ProxyScoringService proxyScoringService;

    @Override
    @Transactional
    public void applyResult(Long proxyId, ProxyCheckResult result) {
        ProxyEntity proxy = proxyRepository.findById(proxyId)
                .orElseThrow(() -> new IllegalArgumentException("Proxy not found: " + proxyId));

        if (result.alive()) {
            proxy.setStatus(ProxyStatus.ALIVE);
            proxy.setLastLatencyMs(result.latencyMs());
            proxy.setScore(proxyScoringService.calculateScore(true, result.latencyMs()));
        } else {
            proxy.setStatus(ProxyStatus.DEAD);
            proxy.setLastLatencyMs(null);
            proxy.setScore(proxyScoringService.calculateScore(false, result.latencyMs()));
        }

        proxyRepository.save(proxy);
    }
}
