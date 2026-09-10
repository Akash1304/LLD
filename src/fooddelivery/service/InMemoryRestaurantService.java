package fooddelivery.service;

import fooddelivery.model.Restaurant;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRestaurantService implements RestaurantService {
    private final Map<String, Restaurant> restaurants = new ConcurrentHashMap<>();

    @Override
    public Restaurant addRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.getId(), restaurant);
        return restaurant;
    }

    @Override
    public Optional<Restaurant> getRestaurant(String restaurantId) {
        return Optional.ofNullable(restaurants.get(restaurantId));
    }
}
