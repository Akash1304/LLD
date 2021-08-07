package com.lld.parkinglot.parkinglot.repository;

import com.lld.parkinglot.parkinglot.entity.Location;
import com.lld.parkinglot.parkinglot.entity.ParkingLot;
import com.lld.parkinglot.parkinglot.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingLotRepo extends JpaRepository<ParkingLot, Integer> {

    @Query(value = "select * from parking_lots where location_id_fk = ?1 ", nativeQuery = true)
    ParkingLot findParkingLotsByLocation(int id);
}