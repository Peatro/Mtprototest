package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.MtProtoDeepProbeResult;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;

public interface MtProtoDeepProbeService {
    MtProtoDeepProbeResult probe(ProxyEntity proxy);
}
