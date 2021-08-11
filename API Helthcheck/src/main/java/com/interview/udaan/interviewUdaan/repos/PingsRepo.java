package com.interview.udaan.interviewUdaan.repos;

import com.interview.udaan.interviewUdaan.entities.Pings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PingsRepo extends JpaRepository<Pings, Integer>{

    @Query(value = "select * from pings where service_id_fk = ?1 and instance_id_fk = ?1 and  timestamp > now() - INTERVAL '1 minutes'",nativeQuery = true)
    List<Pings> findPingsInLast1Min(int serviceId, int instanceId);

    @Query(value = "select * from pings where service_id_fk = ?1 and instance_id_fk = ?1 and  timestamp > now() - INTERVAL '2 minutes'",nativeQuery = true)
    List<Pings> findPingsInLast2Min(int serviceId, int instanceId);

    @Query(value = "select * from pings where service_id_fk = ?1 and instance_id_fk = ?1 and  timestamp > now() - INTERVAL '3 minutes'",nativeQuery = true)
    List<Pings> findPingsInLast3Min(int serviceId, int instanceId);
}
