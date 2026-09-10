package notification.service;

import notification.model.User;

import java.util.Optional;

public interface UserDirectory {
    User addUser(User user);
    Optional<User> getUser(String userId);
}
