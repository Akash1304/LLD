package socialmedia.observer;

// One concrete observer: turns social events into user-facing
// notifications. Stands in for a push/email fan-out; a second listener
// (say, a like-count cache) would subscribe alongside it, untouched by
// this one.
public class NotificationListener implements SocialEventListener {
    @Override
    public void onEvent(SocialEvent event) {
        switch (event.getType()) {
            case LIKE:
                System.out.println("  [notify " + event.getTargetUserId() + "] " + event.getActorId() + " liked your post " + event.getPostId());
                break;
            case COMMENT:
                System.out.println("  [notify " + event.getTargetUserId() + "] " + event.getActorId() + " commented on your post " + event.getPostId());
                break;
            case FOLLOW:
                System.out.println("  [notify " + event.getTargetUserId() + "] " + event.getActorId() + " started following you");
                break;
        }
    }
}
