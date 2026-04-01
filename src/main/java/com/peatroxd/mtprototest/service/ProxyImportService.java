package com.peatroxd.mtprototest.service;

import com.peatroxd.mtprototest.dto.RawProxy;
import com.peatroxd.mtprototest.entity.ProxyEntity;
import com.peatroxd.mtprototest.enums.ProxyStatus;
import com.peatroxd.mtprototest.repository.ProxyRepository;
import com.peatroxd.mtprototest.service.source.ProxySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyImportService {

    private final List<ProxySource> proxySources;
    private final RawProxyNormalizer rawProxyNormalizer;
    private final ProxyRepository proxyRepository;

    public void importAll() {
        for (ProxySource source : proxySources) {
            importFromSource(source);
        }
    }

    public void importFromSource(ProxySource source) {
        List<RawProxy> rawProxies = source.fetch();

        int imported = 0;
        int skipped = 0;

        for (RawProxy rawProxy : rawProxies) {
            var normalizedOpt = rawProxyNormalizer.normalize(rawProxy);

            if (normalizedOpt.isEmpty()) {
                skipped++;
                continue;
            }

            RawProxy normalized = normalizedOpt.get();

            boolean exists = proxyRepository.findByHostAndPortAndSecretAndType(
                    normalized.host(),
                    normalized.port(),
                    normalized.secret(),
                    normalized.type()
            ).isPresent();

            if (exists) {
                skipped++;
                continue;
            }

            ProxyEntity entity = ProxyEntity.builder()
                    .host(normalized.host())
                    .port(normalized.port())
                    .secret(normalized.secret())
                    .type(normalized.type())
                    .source(normalized.source())
                    .status(ProxyStatus.NEW)
                    .score(0)
                    .build();

            proxyRepository.save(entity);
            imported++;
        }

        log.info(
                "Import finished for source='{}': total={}, imported={}, skipped={}",
                source.sourceName(),
                rawProxies.size(),
                imported,
                skipped
        );
    }
}