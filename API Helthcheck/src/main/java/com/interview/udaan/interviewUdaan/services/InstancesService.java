package com.interview.udaan.interviewUdaan.services;

import com.interview.udaan.interviewUdaan.entities.Instances;
import com.interview.udaan.interviewUdaan.repos.InstancesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstancesService {
    @Autowired
    InstancesRepo instancesRepo;

    public Instances findByInstancesName(String name){
        return instancesRepo.findInstancesByName(name);
    }

    public void save(Instances instances){
        instancesRepo.save(instances);
    }
}
