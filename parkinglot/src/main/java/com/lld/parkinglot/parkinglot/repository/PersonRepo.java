package com.lld.parkinglot.parkinglot.repository;

import com.lld.parkinglot.parkinglot.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepo extends JpaRepository<Person, Integer> {
    Person findPersonByName(String name);
}
