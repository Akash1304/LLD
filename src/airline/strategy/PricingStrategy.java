package airline.strategy;

import airline.model.Flight;
import airline.model.Seat;

public interface PricingStrategy {
    double calculatePrice(Flight flight, Seat seat);
}
