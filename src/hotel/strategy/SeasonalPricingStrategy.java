package hotel.strategy;

import hotel.model.RoomType;

import java.time.LocalDate;
import java.time.Month;

// Charges a holiday-season surcharge for any night falling in December or
// January, on top of the room type's base nightly rate.
public class SeasonalPricingStrategy implements PricingStrategy {
    private static final double PEAK_SEASON_SURCHARGE = 0.25;

    @Override
    public double calculatePrice(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        double total = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            double rate = type.getBaseNightlyRate();
            if (night.getMonth() == Month.DECEMBER || night.getMonth() == Month.JANUARY) {
                rate *= (1 + PEAK_SEASON_SURCHARGE);
            }
            total += rate;
        }
        return total;
    }
}
