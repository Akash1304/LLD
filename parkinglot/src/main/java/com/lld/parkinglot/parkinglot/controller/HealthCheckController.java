package com.lld.parkinglot.parkinglot.controller;

import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.repository.PersonRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Autowired
    private PersonRepo personRepo;

    @RequestMapping("/")
    public String healthCheck(){
        return "Hello there";
    }

    @RequestMapping("/call/{name}")
    public String callName(@PathVariable String name){
        Person person = personRepo.findPersonByName(name);
        if(person != null) return "Already present";
        else {
            person = new Person();
            person.setName(name);
            personRepo.save(person);
        }
        return name.toUpperCase() + "is added to db.";
    }
}
