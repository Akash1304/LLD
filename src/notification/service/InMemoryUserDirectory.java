package notification.service;

import notification.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserDirectory implements UserDirectory {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public User addUser(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }
}
