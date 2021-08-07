package com.lld.parkinglot.parkinglot.entity;

import com.lld.parkinglot.parkinglot.enums.BookingStatus;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "acknowledgements")
@Data
public class Acknowledgement extends AbstractEntity {

//    @OneToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "parking_spot_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private ParkingSpot parkingSpot;
    @Column(name = "parking_spot_id")
    private Integer parkingSpotIdFk;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "paymentStatus",nullable = false)
    private BookingStatus bookingStatus;

    @Column(name = "pay_amount")
    private double amount;

    @Column(name = "refund")
    private double refund;
}
