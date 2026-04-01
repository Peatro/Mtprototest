package com.peatroxd.mtprototest.repository;

import com.peatroxd.mtprototest.entity.ProxyEntity;
import com.peatroxd.mtprototest.enums.ProxyStatus;
import com.peatroxd.mtprototest.enums.ProxyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyRepository extends JpaRepository<ProxyEntity, Long> {

    List<ProxyEntity> findTop20ByStatusOrderByScoreDesc(ProxyStatus status);

    Optional<ProxyEntity> findByHostAndPortAndSecretAndType(
            String host,
            Integer port,
            String secret,
            ProxyType type
    );
}
