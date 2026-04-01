package com.peatroxd.mtprototest.service;

import com.peatroxd.mtprototest.entity.ProxyEntity;
import com.peatroxd.mtprototest.enums.ProxyStatus;
import com.peatroxd.mtprototest.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ProxyRepository proxyRepository;

    public List<ProxyEntity> getBest() {
        return proxyRepository.findTop20ByStatusOrderByScoreDesc(ProxyStatus.ALIVE);
    }
}
