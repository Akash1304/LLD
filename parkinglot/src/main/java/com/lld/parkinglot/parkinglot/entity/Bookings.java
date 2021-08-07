package com.lld.parkinglot.parkinglot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "bookings")
public class Bookings extends AbstractEntity {

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "vehicle_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private Vehicle vehicle;

    @Column(name = "vehicle_id_fk",nullable = false)
    private Integer vehicleIdFk;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "parkingspot_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private ParkingSpot parkingSpot;

    @Column(name = "parking_spot_id_fk",nullable = false)
    private Integer parkingSpotIdFk;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ack_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private Acknowledgement acknowledgement;
    @Column(name = "ack_id_fk",nullable = false)
    private Integer ackIdFk;
}
