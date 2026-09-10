package fooddelivery.service;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.Location;

import java.util.List;
import java.util.Optional;

public interface DeliveryAgentRegistry {
    DeliveryAgent registerAgent(DeliveryAgent agent);
    void setAvailability(String agentId, boolean available);
    void updateLocation(String agentId, Location location);
    List<DeliveryAgent> getAvailableAgents();
    Optional<DeliveryAgent> getAgent(String agentId);
}
