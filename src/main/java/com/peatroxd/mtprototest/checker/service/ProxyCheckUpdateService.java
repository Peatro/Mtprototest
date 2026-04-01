package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;

public interface ProxyCheckUpdateService {
    void applyResult(Long proxyId, ProxyCheckResult result);
}
