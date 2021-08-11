package com.interview.udaan.interviewUdaan.repos;

import com.interview.udaan.interviewUdaan.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicesRepo extends JpaRepository<Services, Integer> {

    Services findServicesByName(String name);
}
