package com.peatroxd.mtprototest.dto;

import com.peatroxd.mtprototest.enums.ProxyType;
import lombok.Builder;

@Builder
public record RawProxy(
        String host,
        Integer port,
        String secret,
        ProxyType type,
        String source
) {
}
