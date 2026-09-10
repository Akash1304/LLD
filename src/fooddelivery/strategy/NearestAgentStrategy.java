package fooddelivery.strategy;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.Location;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NearestAgentStrategy implements DeliveryAssignmentStrategy {
    @Override
    public Optional<DeliveryAgent> selectAgent(List<DeliveryAgent> availableAgents, Location restaurantLocation) {
        return availableAgents.stream()
                .min(Comparator.comparingDouble(a -> a.getLocation().distanceTo(restaurantLocation)));
    }
}
