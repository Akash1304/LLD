package socialmedia.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Post {
    private final String id;
    private final String authorId;
    private final String content;
    private final Instant createdAt;
    private final Set<String> likedByUserIds = new LinkedHashSet<>();
    private final List<Comment> comments = new ArrayList<>();

    public Post(String id, String authorId, String content) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    // synchronized on this Post instance -- one monitor per post, so
    // liking/commenting on post A never blocks the same operations on
    // post B, but concurrent likes/comments on the SAME post (and reads
    // of its engagement score while those are in flight) are serialized
    // and can't corrupt the backing Set/List.
    public synchronized void like(String userId) { likedByUserIds.add(userId); }
    public synchronized void unlike(String userId) { likedByUserIds.remove(userId); }
    public synchronized int getLikeCount() { return likedByUserIds.size(); }

    public synchronized void addComment(Comment comment) { comments.add(comment); }
    public synchronized List<Comment> getComments() { return Collections.unmodifiableList(new ArrayList<>(comments)); }

    // simple, transparent measure of how "hot" a post is
    public synchronized int getEngagementScore() { return likedByUserIds.size() + comments.size(); }

    @Override
    public synchronized String toString() {
        return "Post{" + id + ", " + authorId + ": \"" + content + "\", likes=" + likedByUserIds.size() + ", comments=" + comments.size() + '}';
    }
}
