package com.lld.parkinglot.parkinglot.repository;

import com.lld.parkinglot.parkinglot.entity.Bookings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingsRepo extends JpaRepository<Bookings, Integer> {
    @Query(value = "select b from bookings b where b.parking_spot_id_fk = ?1", nativeQuery = true)
    Bookings findBookingsByParkingSpotId(int id);

    @Query(value = "select b from bookings b where b.vehicle_id_fk = ?1",nativeQuery = true)
    Bookings findBookingsByVehicleId(int id);
}
