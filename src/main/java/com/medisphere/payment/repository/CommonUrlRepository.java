package com.medisphere.payment.repository;

import com.medisphere.payment.entity.CommonUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommonUrlRepository extends JpaRepository<CommonUrlEntity, Long> {
    Optional<CommonUrlEntity> findByCode(String code);
}
