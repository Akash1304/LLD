package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Acknowledgement;
import com.lld.parkinglot.parkinglot.entity.Bookings;
import com.lld.parkinglot.parkinglot.entity.Location;
import com.lld.parkinglot.parkinglot.entity.ParkingLot;
import com.lld.parkinglot.parkinglot.entity.ParkingSpot;
import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.entity.Vehicle;
import com.lld.parkinglot.parkinglot.enums.BookingStatus;
import com.lld.parkinglot.parkinglot.enums.VehicleType;
import com.lld.parkinglot.parkinglot.repository.AcknowledgementRepo;
import com.lld.parkinglot.parkinglot.repository.BookingsRepo;
import com.lld.parkinglot.parkinglot.repository.ParkingLotRepo;
import com.lld.parkinglot.parkinglot.repository.ParkingSpotRepo;
import com.lld.parkinglot.parkinglot.repository.VehicleRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParkingService implements ParkingLotService, ParkingSpotManagementService {

    @Autowired
    ParkingLotRepo parkingLotRepo;

    @Autowired
    ParkingSpotRepo parkingSpotRepo;

    @Autowired
    AcknowledgementRepo acknowledgementRepo;

    @Autowired
    BookingService bookingService;

    @Autowired
    VehicleRepo vehicleRepo;

    @Autowired
    UserManagementService userManagementService;

    @Autowired
    BookingsRepo bookingsRepo;


    @Override
    public boolean addParkingLot(Location location, Person manager) {
        ParkingLot parkingLot = getParkingLot(location);
        if (parkingLot != null) return false;
        parkingLot = new ParkingLot();
        parkingLot.setLocationIdFk(location.getId());
        parkingLot.setManagerIdFk(manager.getId());
        parkingLotRepo.save(parkingLot);
        return true;
    }

    @Override
    public ParkingLot getParkingLot(Location location) {
        return parkingLotRepo.findParkingLotsByLocation(location.getId());
    }

    @Override
    public boolean isSpotAvailable(VehicleType vehicleType, Location location) {
        ParkingLot parkingLot = getParkingLot(location);
        if (parkingLot == null) return false;
        ParkingSpot availableSpot = parkingSpotRepo.findFirstAvailableSpotByParkingLotAndVehicleType(parkingLot.getId(), vehicleType.name());
        if(availableSpot == null) return false;
        return true;
    }


    @Override
    public ParkingSpot getNearestSpot(VehicleType vehicleType, ParkingLot parkingLot) {
        return parkingSpotRepo.findFirstAvailableSpotByParkingLotAndVehicleType(parkingLot.getId(), vehicleType.name());
    }

    @Override
    public boolean addParkingSpot(VehicleType vehicleType, Location location) {
        ParkingSpot parkingSpot = new ParkingSpot();
        ParkingLot parkingLot = getParkingLot(location);
        parkingSpot.setParkingLotId(parkingLot.getId());
        parkingSpot.setVehicleType(vehicleType);
        parkingSpotRepo.save(parkingSpot);
        return true;
    }

    @Override
    public boolean vacateSpot(String regNo) {
        Vehicle vehicle = vehicleRepo.findVehicleByRegdNo(regNo);
        Bookings bookings = bookingsRepo.findBookingsByVehicleId(vehicle.getId());
        vehicleRepo.deleteVehicleByRegdNo(regNo);
        bookingsRepo.delete(bookings);
        return false;
    }

    @Override
    public Acknowledgement reserveSpot(Person user, VehicleType vehicleType, String regNo, int amount, Location location) {
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner_id(user.getId());
        vehicle.setRegdNo(regNo);
        vehicle.setType(vehicleType);
        vehicleRepo.save(vehicle);
        Acknowledgement acknowledgement = new Acknowledgement();
        ParkingLot parkingLot = getParkingLot(location);
        ParkingSpot parkingSpot = getNearestSpot(vehicleType, parkingLot);
        if(parkingSpot == null) {
            acknowledgement.setBookingStatus(BookingStatus.invalidBooking);
            acknowledgementRepo.save(acknowledgement);
            return acknowledgement;
        }
        //bookingService.makePayment()
        // booked
        acknowledgement.setAmount(amount);
        acknowledgement.setParkingSpotIdFk(parkingSpot.getId());
        if(amount == vehicle.getType().getParkingFee())
            acknowledgement.setBookingStatus(BookingStatus.paid);
        else if(amount > vehicle.getType().getParkingFee()) {
            acknowledgement.setBookingStatus(BookingStatus.paid);
            acknowledgement.setRefund(amount - vehicle.getType().getParkingFee());
        }else {
            acknowledgement.setBookingStatus(BookingStatus.toBePaid);
        }
        acknowledgementRepo.save(acknowledgement);
        Bookings booking = new Bookings();
        booking.setParkingSpotIdFk(parkingSpot.getId());
        booking.setVehicleIdFk(vehicle.getId());
        booking.setAckIdFk(acknowledgement.getId());
        bookingsRepo.save(booking);
        return acknowledgement;
    }
}
