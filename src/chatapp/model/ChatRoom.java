package chatapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// The Observer "subject": holds membership, message history, and the set
// of currently-subscribed listeners; appending a message notifies every
// listener synchronously (a stand-in for a real-time push/websocket fan-out).
public class ChatRoom {
    private final String id;
    private final String name;
    private final Set<String> memberIds = new LinkedHashSet<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<MessageListener> listeners = new ArrayList<>();

    public ChatRoom(String id, String name, List<String> memberIds) {
        this.id = id;
        this.name = name;
        this.memberIds.addAll(memberIds);
    }

    // Every method that touches memberIds/messages/listeners is
    // synchronized on this ChatRoom instance -- one monitor per room, so
    // sending/joining/subscribing in ROOM A never blocks the same
    // operations in ROOM B, but two concurrent sendMessage calls in the
    // SAME room can't interleave into a corrupted ArrayList or have one
    // thread iterate `listeners` while another is still adding to it.
    public String getId() { return id; }
    public String getName() { return name; }
    public synchronized Set<String> getMemberIds() { return Collections.unmodifiableSet(new LinkedHashSet<>(memberIds)); }
    public synchronized void addMember(String userId) { memberIds.add(userId); }
    public synchronized boolean isMember(String userId) { return memberIds.contains(userId); }
    public synchronized List<Message> getMessages() { return Collections.unmodifiableList(new ArrayList<>(messages)); }

    public synchronized void subscribe(MessageListener listener) { listeners.add(listener); }
    public synchronized void unsubscribe(MessageListener listener) { listeners.remove(listener); }

    public synchronized void appendMessage(Message message) {
        messages.add(message);
        for (MessageListener listener : listeners) listener.onMessage(message);
    }

    @Override
    public synchronized String toString() { return "ChatRoom{" + id + ", " + name + ", members=" + memberIds + '}'; }
}
