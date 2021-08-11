package com.interview.udaan.interviewUdaan.controllers;

import com.interview.udaan.interviewUdaan.services.RegisteringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegisterServiceController {

    @Autowired
    private RegisteringService registeringService;

    @RequestMapping(value = "/register",method = RequestMethod.POST)
    public boolean register(@RequestParam String serviceName, @RequestParam String instanceName){
        return registeringService.register(serviceName, instanceName);
    }
}
