package com.lld.parkinglot.parkinglot.repository;


import com.lld.parkinglot.parkinglot.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepo extends JpaRepository<Vehicle, Integer> {
    void deleteVehicleByRegdNo(String regNo);
    Vehicle findVehicleByRegdNo(String regNo);
}
