package com.peatroxd.mtprototest.checker.service;

import com.peatroxd.mtprototest.checker.model.ProxyCheckResult;
import com.peatroxd.mtprototest.proxy.entity.ProxyEntity;

public interface ProxyConnectivityChecker {
    ProxyCheckResult check(ProxyEntity proxy);
}
