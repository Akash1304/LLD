package com.interview.udaan.interviewUdaan.services;

import com.interview.udaan.interviewUdaan.entities.Services;
import com.interview.udaan.interviewUdaan.repos.ServicesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServicesService {

    @Autowired
    ServicesRepo servicesRepo;

    public Services findServiceByName(String name){
        return servicesRepo.findServicesByName(name);
    }

    public void save(Services service){
        servicesRepo.save(service);
    }

}
