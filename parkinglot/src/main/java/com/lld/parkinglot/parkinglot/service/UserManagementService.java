package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import com.lld.parkinglot.parkinglot.exceptions.MissingUserException;
import com.lld.parkinglot.parkinglot.repository.PersonRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    @Autowired
    private PersonRepo personRepo;

    public Person addUser(String name, AccessType accessType) throws MissingUserException {
        Person person = getUser(name, accessType);
        if(person != null) return person;
        else {
            person = new Person();
            person.setName(name);
            person.setAccessType(accessType);
            personRepo.save(person);
        }
        return person;
    }

    public Person getUser(String name, AccessType accessType) {
        return personRepo.findPersonByNameAndAccessType(name, accessType);
    }
}
