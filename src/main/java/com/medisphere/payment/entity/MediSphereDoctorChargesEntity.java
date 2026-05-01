package com.medisphere.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Entity
@Table(name = "doctor_charges")
public class MediSphereDoctorChargesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "doctor_id", nullable = false)
    private String doctorId;

    @NotNull
    @Column(name = "ms_user_id", nullable = false)
    private String msUserid;

    @NotNull
    @Column(name = "price", nullable = false)
    private String price;

    @ColumnDefault("'LKR'")
    @Column(name = "currency")
    private String currency;

    @Column(name = "status")
    private String status;

    @CreationTimestamp
    @Column(name = "created_date")
    private Instant createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private Instant modifiedDate;


}