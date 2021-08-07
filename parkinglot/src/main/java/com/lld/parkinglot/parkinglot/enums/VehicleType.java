package com.lld.parkinglot.parkinglot.enums;

public enum VehicleType {
    car(100),
    bike(100);

    private int parkingFee;

    VehicleType(int parkingFee) {
        parkingFee = this.parkingFee;
    }

    public int getParkingFee(){
        return this.parkingFee;
    }
}
