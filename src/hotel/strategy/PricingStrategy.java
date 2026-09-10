package hotel.strategy;

import hotel.model.RoomType;

import java.time.LocalDate;

public interface PricingStrategy {
    double calculatePrice(RoomType type, LocalDate checkIn, LocalDate checkOut);
}
