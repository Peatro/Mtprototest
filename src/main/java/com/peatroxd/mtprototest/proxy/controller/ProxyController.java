package com.peatroxd.mtprototest.proxy.controller;

import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proxies")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @GetMapping("/best")
    public List<ProxyResponse> best() {
        return proxyService.getBest();
    }
}
