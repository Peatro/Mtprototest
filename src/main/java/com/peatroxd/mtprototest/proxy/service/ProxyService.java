package com.peatroxd.mtprototest.proxy.service;

import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;

import java.util.List;

public interface ProxyService {
    List<ProxyResponse> getBest();
}
