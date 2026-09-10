package airline.service;

import airline.model.Flight;

import java.util.List;
import java.util.Optional;

public interface FlightService {
    Flight addFlight(Flight flight);
    List<Flight> findFlights(String origin, String destination);
    Optional<Flight> getFlight(String flightId);
}
