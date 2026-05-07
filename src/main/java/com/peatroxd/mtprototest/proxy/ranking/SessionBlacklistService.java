package com.peatroxd.mtprototest.proxy.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionBlacklistService {

    private final ProxyRankingProperties properties;

    // key = windowedSessionKey, value = set of seen proxy IDs
    private final ConcurrentHashMap<String, Set<Long>> sessions = new ConcurrentHashMap<>();
    // key = windowedSessionKey, value = last-access epoch second (for eviction)
    private final ConcurrentHashMap<String, Long> accessTimes = new ConcurrentHashMap<>();

    public Set<Long> getShownProxyIds(String clientKey) {
        if (!properties.sessionBlacklist().enabled()) return Set.of();
        String key = windowedKey(clientKey);
        Set<Long> ids = sessions.get(key);
        if (ids == null) return Set.of();
        accessTimes.put(key, Instant.now().getEpochSecond());
        return Collections.unmodifiableSet(ids);
    }

    public void recordShown(String clientKey, Iterable<Long> proxyIds) {
        if (!properties.sessionBlacklist().enabled()) return;
        String key = windowedKey(clientKey);
        int max = properties.sessionBlacklist().maxHistoryPerSession();
        sessions.compute(key, (k, existing) -> {
            Set<Long> set = existing != null ? existing : ConcurrentHashMap.newKeySet();
            for (Long id : proxyIds) {
                if (set.size() >= max) break;
                set.add(id);
            }
            return set;
        });
        accessTimes.put(key, Instant.now().getEpochSecond());
    }

    @Scheduled(fixedRate = 300_000)
    public void evictExpired() {
        long cutoff = Instant.now().getEpochSecond()
                - (long) properties.sessionBlacklist().windowMinutes() * 60;
        Set<String> expired = new HashSet<>();
        accessTimes.forEach((key, lastAccess) -> {
            if (lastAccess < cutoff) expired.add(key);
        });
        expired.forEach(key -> {
            sessions.remove(key);
            accessTimes.remove(key);
        });
        if (!expired.isEmpty()) {
            log.debug("Session blacklist: evicted {} expired entries", expired.size());
        }
    }

    private String windowedKey(String clientKey) {
        long windowSeconds = (long) properties.sessionBlacklist().windowMinutes() * 60;
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        return clientKey + ":" + bucket;
    }
}
