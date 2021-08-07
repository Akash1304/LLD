package com.lld.parkinglot.parkinglot.exceptions;


public class MissingUserException extends Exception {
    public MissingUserException(String s, String message) {
        super(message);
    }
}
