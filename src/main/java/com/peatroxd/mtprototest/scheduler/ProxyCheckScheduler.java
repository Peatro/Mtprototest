package com.peatroxd.mtprototest.scheduler;

import com.peatroxd.mtprototest.entity.ProxyEntity;
import com.peatroxd.mtprototest.enums.ProxyStatus;
import com.peatroxd.mtprototest.repository.ProxyRepository;
import com.peatroxd.mtprototest.service.ProxyChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProxyCheckScheduler {

    private final ProxyRepository proxyRepository;
    private final ProxyChecker proxyChecker;

    @Scheduled(fixedDelay = 60000)
    public void check() {
        List<ProxyEntity> proxies = proxyRepository.findAll();

        for (ProxyEntity proxy : proxies) {
            boolean alive = proxyChecker.isAlive(proxy.getHost(), proxy.getPort());

            proxy.setStatus(alive ? ProxyStatus.ALIVE : ProxyStatus.DEAD);
            proxy.setScore(alive ? 100 : 0);

            proxyRepository.save(proxy);
        }
    }
}
