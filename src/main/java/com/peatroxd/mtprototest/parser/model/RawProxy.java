package com.peatroxd.mtprototest.parser.model;

import com.peatroxd.mtprototest.proxy.enums.ProxyType;
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
