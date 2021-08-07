package com.lld.parkinglot.parkinglot.entity;

import com.lld.parkinglot.parkinglot.enums.VehicleType;
import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Set;

@Data
@Entity
@Table(name = "parking_spots")
public class ParkingSpot extends AbstractEntity{

    @Enumerated(value = EnumType.STRING)
    @Column(name = "spot_type", nullable = false)
    private VehicleType vehicleType;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "parking_lot_id", insertable = false, updatable = false)
//    private ParkingLot parkingLot;
    @Column(name = "parkinglot_id_fk")
    private int parkingLotId;
}
