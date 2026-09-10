package chatapp.service;

import chatapp.model.Message;

import java.util.List;

public interface MessageService {
    Message sendMessage(String roomId, String senderId, String content) throws NotAMemberException;
    List<Message> getHistory(String roomId, int limit);

    class NotAMemberException extends Exception {
        public NotAMemberException(String message) { super(message); }
    }
}
