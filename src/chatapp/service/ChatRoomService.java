package chatapp.service;

import chatapp.model.ChatRoom;
import chatapp.model.MessageListener;

import java.util.List;
import java.util.Optional;

public interface ChatRoomService {
    ChatRoom createRoom(String name, List<String> memberIds);
    ChatRoom createDirectMessage(String userId1, String userId2);
    void addMember(String roomId, String userId);
    Optional<ChatRoom> getRoom(String roomId);
    void subscribe(String roomId, MessageListener listener);
}
