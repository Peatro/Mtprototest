package com.peatroxd.mtprototest.parser.source;

import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SoliSpiritProxySource implements ProxySource {

    private final RestClient restClient;
    private final String sourceUrl;

    public SoliSpiritProxySource(
            RestClient.Builder restClientBuilder,
            @Value("${app.sources.solispirit.url}") String sourceUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.sourceUrl = sourceUrl;
    }

    @Override
    public List<RawProxy> fetch() {
        log.info("Fetching proxies from {}", sourceUrl);

        String body = restClient.get()
                .uri(sourceUrl)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            log.warn("Received empty body from source {}", sourceName());
            return List.of();
        }

        return body.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(this::parseProxyLink)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public String sourceName() {
        return "solispirit";
    }

    private Optional<RawProxy> parseProxyLink(String link) {
        try {
            URI uri = normalizeTelegramUri(link);

            Map<String, String> params = parseQueryParams(uri.getQuery());

            String server = params.get("server");
            String portRaw = params.get("port");
            String secret = params.get("secret");

            if (server == null || portRaw == null || secret == null) {
                return Optional.empty();
            }

            int port = Integer.parseInt(portRaw);

            return Optional.of(
                    RawProxy.builder()
                            .host(server)
                            .port(port)
                            .secret(secret)
                            .type(ProxyType.MTPROTO)
                            .source(sourceName())
                            .build()
            );

        } catch (Exception e) {
            log.debug("Failed to parse proxy link: {}", link, e);
            return Optional.empty();
        }
    }

    private URI normalizeTelegramUri(String link) {
        String normalized = link.trim();

        if (normalized.startsWith("tg://")) {
            normalized = normalized.replaceFirst("tg://proxy", "https://dummy.local/proxy");
        } else if (normalized.startsWith("https://t.me/proxy")) {
        } else if (normalized.startsWith("http://t.me/proxy")) {
        } else {
            throw new IllegalArgumentException("Unsupported proxy link format: " + normalized);
        }

        return URI.create(normalized);
    }

    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        return Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> decode(parts[0]),
                        parts -> decode(parts[1]),
                        (first, second) -> second,
                        LinkedHashMap::new
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
