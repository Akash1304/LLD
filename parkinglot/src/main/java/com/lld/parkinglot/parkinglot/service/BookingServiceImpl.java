package com.lld.parkinglot.parkinglot.service;

import com.lld.parkinglot.parkinglot.entity.Person;
import org.springframework.stereotype.Service;

@Service
public class BookingServiceImpl implements BookingService{
    @Override
    public boolean makePayment(double amount, Person costumer) {
        return false;
    }
}
