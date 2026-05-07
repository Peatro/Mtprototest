package com.peatroxd.mtprototest.proxy.ranking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @ParameterizedTest
    @CsvSource({
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36, Windows",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36,  Android",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X),        iOS",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7),               macOS",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36,           Linux",
            ",                                                               UNKNOWN",
            "   ,                                                            UNKNOWN"
    })
    void parseOs(String ua, String expectedOs) {
        assertThat(parser.parseOs(ua == null ? null : ua.trim())).isEqualTo(expectedOs.trim());
    }

    @ParameterizedTest
    @CsvSource({
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64),                                DESKTOP",
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) Mobile Safari/537.36,            MOBILE",
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X),                            TABLET",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Mobile Safari,     MOBILE"
    })
    void parseDeviceType(String ua, String expectedType) {
        assertThat(parser.parseDeviceType(ua.trim())).isEqualTo(expectedType.trim());
    }

    @Test
    void nullUaReturnsUnknownForBoth() {
        assertThat(parser.parseOs(null)).isEqualTo("UNKNOWN");
        assertThat(parser.parseDeviceType(null)).isEqualTo("UNKNOWN");
    }
}
