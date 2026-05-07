package com.peatroxd.mtprototest.proxy.ranking;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParser {

    public String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "UNKNOWN";
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("mac os x") || ua.contains("macos")) return "macOS";
        if (ua.contains("linux")) return "Linux";
        return "UNKNOWN";
    }

    public String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "UNKNOWN";
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet")) return "TABLET";
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "MOBILE";
        return "DESKTOP";
    }
}
