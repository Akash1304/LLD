package fooddelivery.service;

import fooddelivery.model.Restaurant;

import java.util.Optional;

public interface RestaurantService {
    Restaurant addRestaurant(Restaurant restaurant);
    Optional<Restaurant> getRestaurant(String restaurantId);
}
