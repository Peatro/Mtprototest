package com.peatroxd.mtprototest.proxy.service.impl;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.mapper.ProxyResponseMapper;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProxyServiceImpl implements ProxyService {

    private final ProxyRepository proxyRepository;
    private final ProxyResponseMapper proxyResponseMapper;

    @Override
    public List<ProxyResponse> getBest() {
        return proxyRepository.findTop50ByStatusOrderByScoreDescLastLatencyMsAsc(ProxyStatus.ALIVE)
                .stream()
                .map(proxyResponseMapper::toResponse)
                .toList();
    }
}
