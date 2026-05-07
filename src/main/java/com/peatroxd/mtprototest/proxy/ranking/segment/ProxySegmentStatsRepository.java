package com.peatroxd.mtprototest.proxy.ranking.segment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProxySegmentStatsRepository extends JpaRepository<ProxySegmentStatsEntity, ProxySegmentStatsId> {

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO proxy_segment_stats (proxy_id, country, os, worked_count, failed_count, updated_at)
            VALUES (:proxyId, :country, :os, :worked, :failed, NOW())
            ON CONFLICT (proxy_id, country, os)
            DO UPDATE SET
                worked_count = proxy_segment_stats.worked_count + EXCLUDED.worked_count,
                failed_count = proxy_segment_stats.failed_count + EXCLUDED.failed_count,
                updated_at   = NOW()
            """)
    void upsertSignal(
            @Param("proxyId") Long proxyId,
            @Param("country") String country,
            @Param("os") String os,
            @Param("worked") long worked,
            @Param("failed") long failed
    );
}
