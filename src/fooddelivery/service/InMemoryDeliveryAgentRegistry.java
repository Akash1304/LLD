package fooddelivery.service;

import fooddelivery.model.DeliveryAgent;
import fooddelivery.model.Location;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryDeliveryAgentRegistry implements DeliveryAgentRegistry {
    private final Map<String, DeliveryAgent> agents = new ConcurrentHashMap<>();

    @Override
    public DeliveryAgent registerAgent(DeliveryAgent agent) {
        agents.put(agent.getId(), agent);
        return agent;
    }

    @Override
    public void setAvailability(String agentId, boolean available) {
        requireAgent(agentId).setAvailable(available);
    }

    @Override
    public void updateLocation(String agentId, Location location) {
        requireAgent(agentId).setLocation(location);
    }

    @Override
    public List<DeliveryAgent> getAvailableAgents() {
        return agents.values().stream().filter(DeliveryAgent::isAvailable).collect(Collectors.toList());
    }

    @Override
    public Optional<DeliveryAgent> getAgent(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    private DeliveryAgent requireAgent(String agentId) {
        DeliveryAgent agent = agents.get(agentId);
        if (agent == null) throw new IllegalArgumentException("Unknown agent: " + agentId);
        return agent;
    }
}
