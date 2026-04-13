package com.medisphere.payment.repository;

import com.medisphere.payment.entity.MedispherePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<MedispherePaymentEntity, Integer> {
    MedispherePaymentEntity findByPaymentReferenceId(String paymentReferenceId);
}
