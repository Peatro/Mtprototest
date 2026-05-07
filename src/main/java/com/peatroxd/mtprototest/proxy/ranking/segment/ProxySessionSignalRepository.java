package com.peatroxd.mtprototest.proxy.ranking.segment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProxySessionSignalRepository extends JpaRepository<ProxySessionSignalEntity, Long> {

    /**
     * Returns session IDs where all events occurred before the given cutoff
     * (i.e., no activity after cutoff — session is considered complete).
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT session_id
            FROM proxy_session_signals
            GROUP BY session_id
            HAVING MAX(occurred_at) < :cutoff
            """)
    List<String> findCompletedSessionIds(@Param("cutoff") LocalDateTime cutoff);

    List<ProxySessionSignalEntity> findAllBySessionIdIn(List<String> sessionIds);

    @Modifying
    @Query("DELETE FROM ProxySessionSignalEntity s WHERE s.sessionId IN :sessionIds")
    void deleteBySessionIdIn(@Param("sessionIds") List<String> sessionIds);
}
