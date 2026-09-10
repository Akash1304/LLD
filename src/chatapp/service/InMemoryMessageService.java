package chatapp.service;

import chatapp.model.ChatRoom;
import chatapp.model.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryMessageService implements MessageService {
    private final ChatRoomService chatRoomService;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryMessageService(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @Override
    public Message sendMessage(String roomId, String senderId, String content) throws NotAMemberException {
        ChatRoom room = chatRoomService.getRoom(roomId)
                .orElseThrow(() -> new NotAMemberException("Unknown room: " + roomId));
        if (!room.isMember(senderId)) {
            throw new NotAMemberException(senderId + " is not a member of room " + roomId);
        }
        Message message = new Message("MSG-" + idCounter.getAndIncrement(), roomId, senderId, content);
        room.appendMessage(message);
        return message;
    }

    @Override
    public List<Message> getHistory(String roomId, int limit) {
        ChatRoom room = chatRoomService.getRoom(roomId).orElseThrow(() -> new IllegalArgumentException("Unknown room: " + roomId));
        List<Message> all = room.getMessages();
        int fromIndex = Math.max(0, all.size() - limit);
        return all.subList(fromIndex, all.size());
    }
}
