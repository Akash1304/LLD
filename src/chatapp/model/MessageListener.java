package chatapp.model;

// Observer interface: a stand-in for a connected client (a websocket
// session, a mobile push token, ...) that wants to be notified the moment
// a new message lands in a room it's subscribed to.
public interface MessageListener {
    void onMessage(Message message);
}
