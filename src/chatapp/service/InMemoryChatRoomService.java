package chatapp.service;

import chatapp.model.ChatRoom;
import chatapp.model.MessageListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryChatRoomService implements ChatRoomService {
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public ChatRoom createRoom(String name, List<String> memberIds) {
        ChatRoom room = new ChatRoom("ROOM-" + idCounter.getAndIncrement(), name, memberIds);
        rooms.put(room.getId(), room);
        return room;
    }

    @Override
    public ChatRoom createDirectMessage(String userId1, String userId2) {
        return createRoom(userId1 + " & " + userId2, Arrays.asList(userId1, userId2));
    }

    @Override
    public void addMember(String roomId, String userId) {
        requireRoom(roomId).addMember(userId);
    }

    @Override
    public Optional<ChatRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public void subscribe(String roomId, MessageListener listener) {
        requireRoom(roomId).subscribe(listener);
    }

    private ChatRoom requireRoom(String roomId) {
        ChatRoom room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("Unknown room: " + roomId);
        return room;
    }
}
