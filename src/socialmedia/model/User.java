package socialmedia.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class User {
    private final String id;
    private final String name;
    private final Set<String> followingIds = new LinkedHashSet<>();
    private final Set<String> followerIds = new LinkedHashSet<>();

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    // synchronized + defensive copies: without this, callers previously
    // got a live reference to the mutable backing Set, so a concurrent
    // follow/unfollow could mutate it while another thread was iterating
    // it (e.g. building a feed from followingIds).
    public synchronized Set<String> getFollowingIds() { return new LinkedHashSet<>(followingIds); }
    public synchronized Set<String> getFollowerIds() { return new LinkedHashSet<>(followerIds); }
    public synchronized void addFollowing(String userId) { followingIds.add(userId); }
    public synchronized void removeFollowing(String userId) { followingIds.remove(userId); }
    public synchronized void addFollower(String userId) { followerIds.add(userId); }
    public synchronized void removeFollower(String userId) { followerIds.remove(userId); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return Objects.equals(id, ((User) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return name; }
}
