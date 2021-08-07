package com.lld.parkinglot.parkinglot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;


@Data
@Entity
@Table(name = "parking_lots")
public class ParkingLot extends AbstractEntity{

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "manager_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private Person manager;

    @Column(name = "manager_id_fk")
    private int managerIdFk;
    @Column(name = "location_id_fk")
    private int locationIdFk;


//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "location_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private Location location;

}
