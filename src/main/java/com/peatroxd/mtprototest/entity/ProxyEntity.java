package com.peatroxd.mtprototest.entity;

import com.peatroxd.mtprototest.enums.ProxyStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proxies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String host;
    private Integer port;
    private String secret;

    @Enumerated(EnumType.STRING)
    private ProxyStatus status;

    private Integer score;

    private Long lastLatencyMs;
}
