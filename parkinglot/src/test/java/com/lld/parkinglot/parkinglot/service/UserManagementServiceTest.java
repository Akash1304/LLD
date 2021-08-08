package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Person;
import com.lld.parkinglot.parkinglot.enums.AccessType;
import com.lld.parkinglot.parkinglot.exceptions.MissingUserException;
import com.lld.parkinglot.parkinglot.repository.PersonRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
class UserManagementServiceTest {

    @Mock
    PersonRepo personRepo;

    @InjectMocks
    UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    void getUser() {
        Person expectedPerson = new Person();
        expectedPerson.setAccessType(AccessType.admin);
        expectedPerson.setName("Akash");
        Mockito.when(personRepo.findPersonByNameAndAccessType("Akash",AccessType.admin)).thenReturn(expectedPerson);

        Person person = userManagementService.getUser("Akash",AccessType.admin);

        assertEquals(expectedPerson.getName(),person.getName());

    }
}