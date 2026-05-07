package com.peatroxd.mtprototest.common.analytics;

import com.peatroxd.mtprototest.proxy.ranking.ProxyRankingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/frontend-config", "/api/v1/frontend-config"})
@EnableConfigurationProperties(PostHogProperties.class)
public class FrontendAnalyticsConfigController {

    private final PostHogProperties postHogProperties;
    private final ProxyRankingProperties rankingProperties;

    public FrontendAnalyticsConfigController(PostHogProperties postHogProperties,
                                              ProxyRankingProperties rankingProperties) {
        this.postHogProperties = postHogProperties;
        this.rankingProperties = rankingProperties;
    }

    @GetMapping
    public FrontendAnalyticsConfigResponse getFrontendConfig() {
        boolean enabled = postHogProperties.isEnabled() && StringUtils.hasText(postHogProperties.getApiKey());

        return new FrontendAnalyticsConfigResponse(
                enabled,
                enabled ? postHogProperties.getApiKey() : null,
                enabled ? postHogProperties.getHost() : null,
                rankingProperties.unrecoverableSession().failureThreshold()
        );
    }
}
