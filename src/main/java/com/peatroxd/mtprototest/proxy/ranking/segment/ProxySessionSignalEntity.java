package com.peatroxd.mtprototest.proxy.ranking.segment;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_session_signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxySessionSignalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "proxy_id", nullable = false)
    private Long proxyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignalEvent event;

    @Column(nullable = false, length = 10)
    private String country;

    @Column(nullable = false, length = 20)
    private String os;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    public void prePersist() {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }
}
