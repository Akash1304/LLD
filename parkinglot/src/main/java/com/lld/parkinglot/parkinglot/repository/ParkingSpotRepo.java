package com.lld.parkinglot.parkinglot.repository;

import com.lld.parkinglot.parkinglot.entity.ParkingLot;
import com.lld.parkinglot.parkinglot.entity.ParkingSpot;
import com.lld.parkinglot.parkinglot.entity.Vehicle;
import com.lld.parkinglot.parkinglot.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingSpotRepo extends JpaRepository<ParkingSpot, Integer> {
    @Query(value = "select * from parking_spots ps left outer join bookings b on ps.id = b.parking_spot_id_fk where ps.parkinglot_id_fk = ?1 and ps.spot_type = ?2 limit 1",nativeQuery = true)
    ParkingSpot findFirstAvailableSpotByParkingLotAndVehicleType(int lotId, String type);

    @Query(value = "select * from parking_spots ps left outer join bookings b on ps.id = b.parking_spot_id_fkwhere ps.spot_type = ?1",nativeQuery = true)
    ParkingSpot findFirstAvailableSpotByVehicleType(String type);

}
