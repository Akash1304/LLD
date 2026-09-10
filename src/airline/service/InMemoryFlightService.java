package airline.service;

import airline.model.Flight;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryFlightService implements FlightService {
    private final Map<String, Flight> flights = new ConcurrentHashMap<>();

    @Override
    public Flight addFlight(Flight flight) {
        flights.put(flight.getId(), flight);
        return flight;
    }

    @Override
    public List<Flight> findFlights(String origin, String destination) {
        return flights.values().stream()
                .filter(f -> f.getOrigin().equalsIgnoreCase(origin) && f.getDestination().equalsIgnoreCase(destination))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Flight> getFlight(String flightId) {
        return Optional.ofNullable(flights.get(flightId));
    }
}
