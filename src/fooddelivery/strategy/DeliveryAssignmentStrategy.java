package fooddelivery.strategy;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.Location;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentStrategy {
    Optional<DeliveryAgent> selectAgent(List<DeliveryAgent> availableAgents, Location restaurantLocation);
}
