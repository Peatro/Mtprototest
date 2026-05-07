package com.peatroxd.mtprototest.proxy.ranking.segment;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProxySegmentStatsId implements Serializable {
    private Long proxyId;
    private String country;
    private String os;
}
