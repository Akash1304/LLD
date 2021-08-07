package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Person;
import org.springframework.stereotype.Service;


public interface BookingService {
    boolean makePayment(double amount, Person costumer);
}
