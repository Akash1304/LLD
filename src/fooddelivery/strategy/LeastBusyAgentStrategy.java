package fooddelivery.strategy;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.Location;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Spreads work evenly across the fleet by picking whichever available
// agent has completed the fewest deliveries so far, tie-broken by
// distance -- trades "fastest pickup for this one order" for fairer
// earnings distribution across agents over a shift.
public class LeastBusyAgentStrategy implements DeliveryAssignmentStrategy {
    @Override
    public Optional<DeliveryAgent> selectAgent(List<DeliveryAgent> availableAgents, Location restaurantLocation) {
        return availableAgents.stream()
                .min(Comparator.comparingInt(DeliveryAgent::getCompletedDeliveries)
                        .thenComparingDouble(a -> a.getLocation().distanceTo(restaurantLocation)));
    }
}
