package com.peatroxd.mtprototest.proxy.ranking;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeoIpResolver {

    // Standard headers set by Cloudflare, nginx geo module, or CDNs
    private static final List<String> COUNTRY_HEADERS = List.of(
            "CF-IPCountry",
            "X-Country-Code",
            "X-Geoip-Country",
            "X-GEO-Country"
    );

    public String resolve(HttpServletRequest request) {
        for (String header : COUNTRY_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !value.equals("XX")) {
                return value.toUpperCase().trim();
            }
        }
        return "UNKNOWN";
    }
}
