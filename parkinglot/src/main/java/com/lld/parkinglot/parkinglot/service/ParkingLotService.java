package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Location;
import com.lld.parkinglot.parkinglot.entity.ParkingLot;
import com.lld.parkinglot.parkinglot.entity.Person;

public interface ParkingLotService {
    boolean addParkingLot(Location location, Person manager);
    ParkingLot getParkingLot(Location location);
}
