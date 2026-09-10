package airline.strategy;

import airline.model.Flight;
import airline.model.Seat;
import airline.model.SeatClass;

import java.util.List;

// Adds a surcharge that grows with how full a seat class already is, so
// the last few seats in a nearly-sold-out class cost more than the first
// ones -- a simple stand-in for real airline dynamic pricing.
public class DemandBasedPricingStrategy implements PricingStrategy {
    private static final double MAX_SURCHARGE = 0.50; // up to +50% when the class is full

    @Override
    public double calculatePrice(Flight flight, Seat seat) {
        SeatClass seatClass = seat.getSeatClass();
        List<Seat> classSeats = flight.getSeatsByClass(seatClass);
        int totalInClass = classSeats.size();
        int bookedInClass = flight.getBookedCountForClass(seatClass);

        double occupancyRatio = totalInClass == 0 ? 0 : (double) bookedInClass / totalInClass;
        double surchargeMultiplier = 1 + (occupancyRatio * MAX_SURCHARGE);

        return flight.getBasePrice() * seatClass.getPriceMultiplier() * surchargeMultiplier;
    }
}
