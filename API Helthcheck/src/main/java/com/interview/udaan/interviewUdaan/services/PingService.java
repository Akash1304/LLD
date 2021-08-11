package com.interview.udaan.interviewUdaan.services;

import com.interview.udaan.interviewUdaan.entities.Instances;
import com.interview.udaan.interviewUdaan.entities.Pings;
import com.interview.udaan.interviewUdaan.entities.Services;
import com.interview.udaan.interviewUdaan.repos.PingsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PingService {

    @Autowired
    InstancesService instancesService;

    @Autowired
    ServicesService servicesService;

    @Autowired
    PingsRepo pingsRepo;

    public boolean registerPing(String serviceName, String instanceName){
        Services service = servicesService.findServiceByName(serviceName);
        if(service == null) return false;
        Instances instances = instancesService.findByInstancesName(instanceName);
        if(instances == null) return false;
        Pings ping = new Pings();
        ping.setInstanceIdFk(instances.getId());
        ping.setServiceIdFk(service.getId());
        pingsRepo.save(ping);
        return true;
    }
}
