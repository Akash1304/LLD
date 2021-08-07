package com.lld.parkinglot.parkinglot.controller;

import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import com.lld.parkinglot.parkinglot.exceptions.MissingUserException;
import com.lld.parkinglot.parkinglot.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "/user")
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public Person createUser(@RequestParam String name, @RequestParam AccessType accessType) throws MissingUserException {
        return userManagementService.addUser(name, accessType);
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public Person getUser(@RequestParam String name, @RequestParam AccessType accessType) throws MissingUserException {
        return userManagementService.getUser(name, accessType);
    }
}
