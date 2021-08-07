package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Acknowledgement;
import com.lld.parkinglot.parkinglot.entity.Location;
import com.lld.parkinglot.parkinglot.entity.ParkingLot;
import com.lld.parkinglot.parkinglot.entity.ParkingSpot;
import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.entity.Vehicle;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import com.lld.parkinglot.parkinglot.enums.VehicleType;

/**
 * Add person with access
 * Admin, manager can create slots and add spaces but cannot book
 * customer can book
 *
 * Get nearest empty lot
 * Book lot
 * Get Invoice
 * check for empty lot
 */

public interface ParkingSpotManagementService {
    boolean isSpotAvailable(VehicleType vehicleType, Location location);
    ParkingSpot getNearestSpot(VehicleType vehicleType, ParkingLot parkingLot);
    boolean addParkingSpot(VehicleType vehicleType, Location location);
    boolean vacateSpot(String reg_no);
    Acknowledgement reserveSpot(Person user, VehicleType vehicleType, String regNo, int amount, Location location);
}
