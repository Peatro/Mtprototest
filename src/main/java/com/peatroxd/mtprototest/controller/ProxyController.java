package com.peatroxd.mtprototest.controller;

import com.peatroxd.mtprototest.entity.ProxyEntity;
import com.peatroxd.mtprototest.service.ProxyService;
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
    public List<ProxyEntity> best() {
        return proxyService.getBest();
    }
}
