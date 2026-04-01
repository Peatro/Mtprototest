package com.peatroxd.mtprototest.proxy.dto.response;

import lombok.Builder;

@Builder
public record ProxyResponse(
        Long id,
        String host,
        Integer port,
        String secret,
        String status,
        Integer score,
        Long lastLatencyMs,
        String telegramDeepLink,
        String telegramWebLink
) {
}
