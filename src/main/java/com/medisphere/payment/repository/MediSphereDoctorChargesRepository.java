package com.medisphere.payment.repository;

import com.medisphere.payment.entity.MediSphereDoctorChargesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediSphereDoctorChargesRepository extends JpaRepository<MediSphereDoctorChargesEntity, Integer> {
    Optional<MediSphereDoctorChargesEntity> findByDoctorId(String doctorId);
}
