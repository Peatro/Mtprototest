package com.peatroxd.mtprototest.proxy.ranking.segment;

import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
import com.peatroxd.mtprototest.proxy.ranking.GeoIpResolver;
import com.peatroxd.mtprototest.proxy.ranking.UserAgentParser;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequestMapping({"/api/proxies", "/api/v1/proxies", "/api/public/proxies"})
@RequiredArgsConstructor
public class ProxySignalController {

    private final ProxyService proxyService;
    private final ProxySegmentStatsService segmentStatsService;
    private final GeoIpResolver geoIpResolver;
    private final UserAgentParser userAgentParser;
    private final ClientRequestKeyResolver clientRequestKeyResolver;

    @PostMapping("/{proxyId}/signal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordSignal(
            @PathVariable Long proxyId,
            @Valid @RequestBody SignalRequest body,
            HttpServletRequest request
    ) {
        // Validate proxy exists (throws 404 if not found)
        proxyService.getById(proxyId);

        String country = geoIpResolver.resolve(request);
        String os = userAgentParser.parseOs(request.getHeader("User-Agent"));

        try {
            segmentStatsService.recordSignal(proxyId, country, os, body.event());
        } catch (Exception e) {
            log.warn("Failed to record segment signal for proxy {}: {}", proxyId, e.getMessage());
        }
    }
}
