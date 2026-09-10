package chatapp.model;

import java.time.Instant;

public class Message {
    private final String id;
    private final String chatRoomId;
    private final String senderId;
    private final String content;
    private final Instant sentAt;

    public Message(String id, String chatRoomId, String senderId, String content) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.sentAt = Instant.now();
    }

    public String getId() { return id; }
    public String getChatRoomId() { return chatRoomId; }
    public String getSenderId() { return senderId; }
    public String getContent() { return content; }
    public Instant getSentAt() { return sentAt; }

    @Override
    public String toString() { return senderId + ": " + content; }
}
