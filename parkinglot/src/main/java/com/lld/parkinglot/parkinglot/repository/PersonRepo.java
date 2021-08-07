package com.lld.parkinglot.parkinglot.repository;

import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepo extends JpaRepository<Person, Integer> {
    Person findPersonByNameAndAccessType(String name, AccessType accessType);
}
