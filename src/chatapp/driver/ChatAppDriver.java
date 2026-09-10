package chatapp.driver;

import chatapp.model.ChatRoom;
import chatapp.model.Message;
import chatapp.service.ChatRoomService;
import chatapp.service.InMemoryChatRoomService;
import chatapp.service.InMemoryMessageService;
import chatapp.service.MessageService;

import java.util.Arrays;
import java.util.List;

public class ChatAppDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        ChatRoomService chatRoomService = new InMemoryChatRoomService();
        MessageService messageService = new InMemoryMessageService(chatRoomService);

        ChatRoom room = chatRoomService.createRoom("Engineering", Arrays.asList("alice", "bob"));
        System.out.println("Created: " + room);

        // simulate Bob's phone being subscribed for real-time push
        chatRoomService.subscribe(room.getId(), message ->
                System.out.println("  [Bob's phone buzzes] New message from " + message.getSenderId() + ": " + message.getContent()));

        try {
            System.out.println("\nAlice sends a message (Bob's listener should fire immediately):");
            messageService.sendMessage(room.getId(), "alice", "Standup in 5 minutes");

            System.out.println("\nCarol (not a member) tries to send a message:");
            try {
                messageService.sendMessage(room.getId(), "carol", "Can I join?");
            } catch (MessageService.NotAMemberException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nAlice adds Carol to the room, then Carol can send:");
            chatRoomService.addMember(room.getId(), "carol");
            messageService.sendMessage(room.getId(), "carol", "Thanks for the add!");

            System.out.println("\nFull message history: " + messageService.getHistory(room.getId(), 10));

            System.out.println("\nOne-on-one direct message between Alice and Dave:");
            ChatRoom dm = chatRoomService.createDirectMessage("alice", "dave");
            messageService.sendMessage(dm.getId(), "dave", "Hey, got a minute?");
            List<Message> dmHistory = messageService.getHistory(dm.getId(), 10);
            System.out.println(dmHistory);
        } catch (MessageService.NotAMemberException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
