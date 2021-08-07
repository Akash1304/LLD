package com.lld.parkinglot.parkinglot.controller;

import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import com.lld.parkinglot.parkinglot.repository.PersonRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Autowired
    private PersonRepo personRepo;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String healthCheck(){
        return "Hello there";
    }

    @RequestMapping(value = "/call", method = RequestMethod.GET)
    public String callName(@RequestParam String name, @RequestParam AccessType accessType){
        Person person = personRepo.findPersonByNameAndAccessType(name, accessType);
        if(person != null) return "Already present";
        else {
            person = new Person();
            person.setName(name);
            person.setAccessType(accessType);
            personRepo.save(person);
        }
        return name.toUpperCase() + "is added to db.";
    }
}
