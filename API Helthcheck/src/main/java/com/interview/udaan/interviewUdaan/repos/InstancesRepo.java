package com.interview.udaan.interviewUdaan.repos;

import com.interview.udaan.interviewUdaan.entities.Instances;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstancesRepo extends JpaRepository<Instances, Integer> {

    Instances findInstancesByName(String name);

    List<Instances> findAllByServiceIdFk(int serviceIDFk);
}
