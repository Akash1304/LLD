package com.lld.parkinglot.parkinglot.repository;

import com.lld.parkinglot.parkinglot.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepo extends JpaRepository<Location, Integer> {
    Location findLocationByLongitudeAndLatitude(int longitude, int lat);
}
