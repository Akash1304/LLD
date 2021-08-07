package com.lld.parkinglot.parkinglot.entity;

import com.lld.parkinglot.parkinglot.enums.VehicleType;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle extends AbstractEntity{

    @Enumerated(value = EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType type;

    @Column(name = "regd_no")
    private String regdNo;

//    @OneToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "owner_id", referencedColumnName = "id")
//    private Person owner;
    @Column(name = "owner_id_fk")
    private int owner_id;
}
