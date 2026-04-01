package com.peatroxd.mtprototest.repository;

import com.peatroxd.mtprototest.entity.ProxyEntity;
import com.peatroxd.mtprototest.enums.ProxyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProxyRepository extends JpaRepository<ProxyEntity, Long> {

    List<ProxyEntity> findTop20ByStatusOrderByScoreDesc(ProxyStatus status);
}
