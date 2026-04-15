package com.peatroxd.mtprototest.proxy.dto.response;

/**
 * Lightweight telegram-ready proxy DTO.
 * {@code tgLink} is a fully assembled {@code tg://proxy?server=...} deep link
 * that can be posted to a Telegram channel or sent to a bot as-is.
 */
public record ProxyTelegramLinkDto(
        String server,
        int port,
        String secret,
        Long latencyMs,
        String tgLink
) {
}
