package com.medisphere.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medisphere_payment")
public class MedispherePaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @NotNull
    @Column(name = "payment_reference_id", nullable = false, unique = true)
    private String paymentReferenceId;

    @NotNull
    @Column(name = "appointment_reference_id", nullable = false)
    private String appointmentReferenceId;

    @NotNull
    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @NotNull
    @Column(name = "ms_user_id", nullable = false)
    private String msUserId;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private String amount;

    @Column(name = "currency", length = 10)
    private String currency = "LKR";

    @Column(name = "status", length = 20)
    private String status; // PENDING, SUCCESS, FAILED, CANCELLED

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payhere_payment_id")
    private String payherePaymentId;

    @Column(name = "payhere_amount", precision = 10, scale = 2)
    private String payhereAmount;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}