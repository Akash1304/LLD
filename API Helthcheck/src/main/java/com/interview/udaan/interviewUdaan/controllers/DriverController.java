package com.interview.udaan.interviewUdaan.controllers;

import com.interview.udaan.interviewUdaan.services.HealthCheckService;
import com.interview.udaan.interviewUdaan.services.PingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DriverController {

    @Autowired
    PingService pingService;

    @Autowired
    HealthCheckService healthCheckService;

    @RequestMapping(value = "/ping",method = RequestMethod.POST)
    public boolean ping(@RequestParam  String serviceName,@RequestParam String instanceName){
        return pingService.registerPing(serviceName,instanceName);
    }

    @RequestMapping(value = "/healthCheck",method = RequestMethod.GET)
    public boolean checkAndUpdateHealth(){
        healthCheckService.checkAndUpdateHealth();
        return true;
    }
}
