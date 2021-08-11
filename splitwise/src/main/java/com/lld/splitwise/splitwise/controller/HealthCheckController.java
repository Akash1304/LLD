package com.lld.splitwise.splitwise.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @RequestMapping(value = "/",method = RequestMethod.GET)
    public String sayHello(){
        return "Hello!!";
    }
}
