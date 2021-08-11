package com.interview.udaan.interviewUdaan.services;

import com.interview.udaan.interviewUdaan.entities.Instances;
import com.interview.udaan.interviewUdaan.entities.Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegisteringService {

    @Autowired
    InstancesService instancesService;

    @Autowired
    ServicesService servicesService;

    public boolean register(String serviceName, String instanceName){
        Services services = registerService(serviceName);
        registerInstance(instanceName, services.getId());
        return true;
    }

    private void registerInstance(String instanceName, int serviceIdFk) {
        Instances instance = instancesService.findByInstancesName(instanceName);
        if(instance == null){
            instance = new Instances();
            instance.setName(instanceName);
            instance.setServiceIdFk(serviceIdFk);
            instancesService.save(instance);
        }
    }

    private Services registerService(String serviceName) {
        Services service = servicesService.findServiceByName(serviceName);
        if(service == null){
            service = new Services();
            service.setName(serviceName);
            servicesService.save(service);
        }
        return service;
    }
}
