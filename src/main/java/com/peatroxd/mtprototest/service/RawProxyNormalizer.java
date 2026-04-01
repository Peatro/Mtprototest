package com.peatroxd.mtprototest.service;

import com.peatroxd.mtprototest.dto.RawProxy;
import com.peatroxd.mtprototest.enums.ProxyType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RawProxyNormalizer {

    public Optional<RawProxy> normalize(RawProxy rawProxy) {
        if (rawProxy == null) {
            return Optional.empty();
        }

        String host = normalizeHost(rawProxy.host());
        Integer port = rawProxy.port();
        String secret = normalizeSecret(rawProxy.secret());

        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        if (port == null || port < 1 || port > 65535) {
            return Optional.empty();
        }

        if (rawProxy.type() == ProxyType.MTPROTO && (secret == null || secret.isBlank())) {
            return Optional.empty();
        }

        return Optional.of(
                RawProxy.builder()
                        .host(host)
                        .port(port)
                        .secret(secret)
                        .type(rawProxy.type())
                        .source(rawProxy.source())
                        .build()
        );
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }

        String normalized = host.trim();

        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String normalizeSecret(String secret) {
        return secret == null ? null : secret.trim();
    }
}
