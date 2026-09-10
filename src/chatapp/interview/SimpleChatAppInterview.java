package chatapp.interview;

import java.util.*;

// Compact, single-file interview-friendly chat demo.
// Supports: a room with members, sending a message with a membership
// check, and an observer-style listener for real-time delivery.
public class SimpleChatAppInterview {

    interface Listener { void onMessage(String sender, String content); }

    static class Room {
        final String id;
        final Set<String> members = new LinkedHashSet<>();
        final List<String> history = new ArrayList<>();
        final List<Listener> listeners = new ArrayList<>();
        Room(String id, Collection<String> members) { this.id = id; this.members.addAll(members); }
    }

    final Map<String, Room> rooms = new HashMap<>();
    int counter = 1;

    Room createRoom(Collection<String> members) {
        Room room = new Room("ROOM-" + counter++, members);
        rooms.put(room.id, room);
        return room;
    }

    void subscribe(String roomId, Listener listener) { rooms.get(roomId).listeners.add(listener); }

    void sendMessage(String roomId, String sender, String content) throws Exception {
        Room room = rooms.get(roomId);
        if (!room.members.contains(sender)) throw new Exception(sender + " is not a member of " + roomId);
        String entry = sender + ": " + content;
        room.history.add(entry);
        for (Listener l : room.listeners) l.onMessage(sender, content);
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleChatAppInterview chat = new SimpleChatAppInterview();
        System.out.println("== Simple Chat App Interview Demo ==");

        Room room = chat.createRoom(Arrays.asList("alice", "bob"));
        chat.subscribe(room.id, (sender, content) ->
                System.out.println("  [Bob's phone buzzes] " + sender + ": " + content));

        try {
            System.out.println("Alice sends a message:");
            chat.sendMessage(room.id, "alice", "Standup in 5 minutes");

            System.out.println("\nCarol (not a member) tries to send:");
            try {
                chat.sendMessage(room.id, "carol", "Can I join?");
            } catch (Exception e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nHistory: " + room.history);
        } catch (Exception e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
