package socialmedia.service;

import socialmedia.model.User;

import java.util.Optional;

public interface UserService {
    User createUser(String id, String name);
    void follow(String followerId, String followeeId);
    void unfollow(String followerId, String followeeId);
    Optional<User> getUser(String userId);
}
