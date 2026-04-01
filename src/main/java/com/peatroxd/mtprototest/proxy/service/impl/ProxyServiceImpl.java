package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.mapper.ProxyResponseMapper;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyServiceImpl implements ProxyService {

    private static final int BEST_PROXY_LIMIT = 50;

    private final ProxyRepository proxyRepository;
    private final ProxyResponseMapper proxyResponseMapper;

    @Override
    public List<ProxyResponse> getBest() {
        return proxyRepository.findTop200ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus.ALIVE)
                .stream()
                .sorted(bestProxyComparator())
                .limit(BEST_PROXY_LIMIT)
                .map(proxyResponseMapper::toResponse)
                .toList();
    }

    private Comparator<ProxyEntity> bestProxyComparator() {
        return Comparator
                .comparingInt(this::verificationRank)
                .thenComparing(ProxyEntity::getScore, Comparator.reverseOrder())
                .thenComparing(proxy -> proxy.getLastLatencyMs() == null ? Long.MAX_VALUE : proxy.getLastLatencyMs())
                .thenComparing(ProxyEntity::getId);
    }

    private int verificationRank(ProxyEntity proxy) {
        return switch (proxy.getVerificationStatus()) {
            case VERIFIED -> 0;
            case QUICK_OK -> 1;
            case UNVERIFIED -> 2;
        };
    }
}
