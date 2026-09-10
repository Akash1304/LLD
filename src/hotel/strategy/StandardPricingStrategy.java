package hotel.strategy;

import hotel.model.RoomType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StandardPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return nights * type.getBaseNightlyRate();
    }
}
