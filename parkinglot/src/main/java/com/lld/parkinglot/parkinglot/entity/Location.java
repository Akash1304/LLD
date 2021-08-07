package com.lld.parkinglot.parkinglot.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "locations")
public class Location extends AbstractEntity {

    @Column(name = "latitude")
    private int latitude;
    @Column(name = "longitude")
    private int longitude;
}
