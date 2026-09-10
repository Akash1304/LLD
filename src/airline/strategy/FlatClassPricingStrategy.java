package airline.strategy;

import airline.model.Flight;
import airline.model.Seat;

public class FlatClassPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(Flight flight, Seat seat) {
        return flight.getBasePrice() * seat.getSeatClass().getPriceMultiplier();
    }
}
