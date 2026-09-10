package socialmedia.observer;

// Observer contract. One method taking an event object (rather than
// onLike/onComment/onFollow) so adding a new event type (SHARE, MENTION)
// doesn't force every existing listener to implement a new method.
public interface SocialEventListener {
    void onEvent(SocialEvent event);
}
