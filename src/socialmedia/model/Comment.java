package socialmedia.model;

import java.time.Instant;

public class Comment {
    private final String id;
    private final String authorId;
    private final String content;
    private final Instant createdAt;

    public Comment(String id, String authorId, String content) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public String toString() { return authorId + ": " + content; }
}
