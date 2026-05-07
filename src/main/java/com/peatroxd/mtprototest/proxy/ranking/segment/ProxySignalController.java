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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@RestController
@Validated
@RequestMapping({"/api/proxies", "/api/v1/proxies", "/api/public/proxies"})
@RequiredArgsConstructor
public class ProxySignalController {

    private static final long SESSION_BUCKET_SECONDS = 3600; // 1-hour session window

    private final ProxyService proxyService;
    private final ProxySegmentStatsService segmentStatsService;
    private final ProxySessionSignalRepository sessionSignalRepository;
    private final GeoIpResolver geoIpResolver;
    private final UserAgentParser userAgentParser;
    private final ClientRequestKeyResolver clientRequestKeyResolver;

    @PostMapping("/{proxyId}/signal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void recordSignal(
            @PathVariable Long proxyId,
            @Valid @RequestBody SignalRequest body,
            HttpServletRequest request
    ) {
        proxyService.getById(proxyId);

        String country = geoIpResolver.resolve(request);
        String os = userAgentParser.parseOs(request.getHeader("User-Agent"));
        String clientKey = clientRequestKeyResolver.resolve(request);
        String sessionId = deriveSessionId(clientKey);
        SignalEvent event = body.event();

        try {
            // All events go to the session signals table for composite analysis
            sessionSignalRepository.save(ProxySessionSignalEntity.builder()
                    .sessionId(sessionId)
                    .proxyId(proxyId)
                    .event(event)
                    .country(country)
                    .os(os)
                    .occurredAt(LocalDateTime.now())
                    .build());

            // WORKED/FAILED also update proxy_segment_stats immediately for low-latency effect
            if (event == SignalEvent.WORKED || event == SignalEvent.FAILED) {
                segmentStatsService.recordDirectSignal(proxyId, country, os, event);
            }
        } catch (Exception e) {
            log.warn("Failed to record signal {} for proxy {}: {}", event, proxyId, e.getMessage());
        }
    }

    private String deriveSessionId(String clientKey) {
        long bucket = Instant.now().getEpochSecond() / SESSION_BUCKET_SECONDS;
        return clientKey + ":" + bucket;
    }
}
