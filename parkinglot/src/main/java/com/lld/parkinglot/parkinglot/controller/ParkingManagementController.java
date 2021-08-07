package com.lld.parkinglot.parkinglot.controller;

import com.lld.parkinglot.parkinglot.entity.Acknowledgement;
import com.lld.parkinglot.parkinglot.entity.Location;
import com.lld.parkinglot.parkinglot.entity.ParkingLot;
import com.lld.parkinglot.parkinglot.entity.ParkingSpot;
import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import com.lld.parkinglot.parkinglot.enums.VehicleType;
import com.lld.parkinglot.parkinglot.exceptions.MissingUserException;
import com.lld.parkinglot.parkinglot.repository.LocationRepo;
import com.lld.parkinglot.parkinglot.service.ParkingService;

import com.lld.parkinglot.parkinglot.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manage")
public class ParkingManagementController {

    @Autowired
    ParkingService parkingService;

    @Autowired
    UserManagementService userManagementService;

    @Autowired
    LocationRepo locationRepo;

    @RequestMapping(value = "/addLot", method = RequestMethod.POST)
    public String addParkingLot (@RequestParam int longitude, @RequestParam int lattitude,@RequestParam String name, @RequestParam AccessType accessType ) throws MissingUserException {
        Location location = new Location();
        location.setLatitude(lattitude);
        location.setLongitude(longitude);
        locationRepo.save(location);

        Person manager = userManagementService.addUser(name, accessType);
        System.out.println(manager.getAccessType());
        if(manager.getAccessType() != AccessType.admin && manager.getAccessType() != AccessType.manager) {
            return "Unauthorized access";
        }
        parkingService.addParkingLot(location, manager);
        return "Parking lot added successfully";
    }

    @RequestMapping(value = "/addSpot", method = RequestMethod.POST)
    public String addParkingSpot (@RequestParam VehicleType vehicleType, @RequestParam int longitude, @RequestParam int lattitude,@RequestParam String name, @RequestParam AccessType accessType) throws MissingUserException {
        Location location = locationRepo.findLocationByLongitudeAndLatitude(longitude,lattitude);
        Person manager = userManagementService.addUser(name, accessType);
        if(manager.getAccessType() != AccessType.admin && manager.getAccessType() != AccessType.manager)
            return "Unauthorized access";
        parkingService.addParkingSpot(vehicleType, location);
        return "Parking spot added successfully";
    }

    @RequestMapping(value = "/checkIfSpotAvailable", method = RequestMethod.GET)
    public boolean checkIfSpotAvailable (@RequestParam VehicleType vehicleType, @RequestParam int longitude, @RequestParam int lattitude){
        Location location = locationRepo.findLocationByLongitudeAndLatitude(longitude,lattitude);
        return parkingService.isSpotAvailable(vehicleType, location);
    }

    @RequestMapping(value = "/reserveSpot", method = RequestMethod.POST)
    public Acknowledgement reserveSpot (@RequestParam VehicleType vehicleType, @RequestParam int amount, @RequestParam String name,
                                        @RequestParam String regNo, @RequestParam int longitude, @RequestParam int lattitude) throws MissingUserException {
        Person user = userManagementService.getUser(name, AccessType.registered_customer);
        if(user == null) user = userManagementService.addUser(name, AccessType.registered_customer);
        Location location = locationRepo.findLocationByLongitudeAndLatitude(longitude,lattitude);
        return parkingService.reserveSpot(user,vehicleType,regNo,amount,location);
    }

    @RequestMapping(value = "/vacateSpot", method = RequestMethod.DELETE)
    public boolean vacateSpot (@RequestParam String regNo){
        return parkingService.vacateSpot(regNo);
    }
}
