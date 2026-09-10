package socialmedia.observer;

// Immutable event payload published by the services. `targetUserId` is
// whoever should care (the post's author for LIKE/COMMENT, the followee for
// FOLLOW); `postId` is empty for FOLLOW.
public class SocialEvent {
    public enum Type { LIKE, COMMENT, FOLLOW }

    private final Type type;
    private final String actorId;
    private final String targetUserId;
    private final String postId;

    public SocialEvent(Type type, String actorId, String targetUserId, String postId) {
        this.type = type;
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.postId = postId;
    }

    public Type getType() { return type; }
    public String getActorId() { return actorId; }
    public String getTargetUserId() { return targetUserId; }
    public String getPostId() { return postId; }
}
