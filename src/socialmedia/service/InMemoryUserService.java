package socialmedia.service;

import socialmedia.model.User;
import socialmedia.observer.SocialEvent;
import socialmedia.observer.SocialEventPublisher;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserService implements UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final SocialEventPublisher publisher;

    public InMemoryUserService(SocialEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public User createUser(String id, String name) {
        User user = new User(id, name);
        users.put(id, user);
        return user;
    }

    // follow/unfollow touch TWO users' sets together (A's followingIds and
    // B's followerIds). Each individual mutator on User is synchronized on
    // that user, but updating both consistently as a pair means acquiring
    // both locks -- and always in the SAME global order (lexicographic by
    // id), never "whichever user is the follower first". Without a fixed
    // order, a concurrent follow(A, B) and follow(B, A) could each grab
    // one lock and then block forever waiting for the other -- a classic
    // lock-ordering deadlock.
    @Override
    public void follow(String followerId, String followeeId) {
        User follower = requireUser(followerId);
        User followee = requireUser(followeeId);
        withOrderedLocks(follower, followee, () -> {
            follower.addFollowing(followeeId);
            followee.addFollower(followerId);
        });
        // published after both locks are released -- never notify while
        // holding two user monitors
        publisher.publish(new SocialEvent(SocialEvent.Type.FOLLOW, followerId, followeeId, null));
    }

    @Override
    public void unfollow(String followerId, String followeeId) {
        User follower = requireUser(followerId);
        User followee = requireUser(followeeId);
        withOrderedLocks(follower, followee, () -> {
            follower.removeFollowing(followeeId);
            followee.removeFollower(followerId);
        });
    }

    private void withOrderedLocks(User a, User b, Runnable action) {
        User first = a.getId().compareTo(b.getId()) <= 0 ? a : b;
        User second = first == a ? b : a;
        synchronized (first) {
            synchronized (second) {
                action.run();
            }
        }
    }

    @Override
    public Optional<User> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    private User requireUser(String userId) {
        User user = users.get(userId);
        if (user == null) throw new IllegalArgumentException("Unknown user: " + userId);
        return user;
    }
}
