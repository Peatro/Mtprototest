package com.peatroxd.mtprototest.proxy.ranking.segment;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_segment_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxySegmentStatsEntity {

    @EmbeddedId
    private ProxySegmentStatsId id;

    @Column(name = "worked_count", nullable = false)
    private long workedCount;

    @Column(name = "failed_count", nullable = false)
    private long failedCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
