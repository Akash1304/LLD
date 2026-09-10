package notification.interview;

import java.util.*;

// Compact, single-file interview-friendly notification demo.
// Supports: per-user channel fallback order, retries per channel, and a
// deterministic flaky channel to exercise the retry path.
public class SimpleNotificationInterview {
    enum Channel { PUSH, EMAIL, SMS }

    interface Sender { void send(String userId, String message) throws Exception; }

    static Map<String, Integer> pushAttempts = new HashMap<>();

    static Sender flakyPush(int failuresBeforeSuccess) {
        return (userId, message) -> {
            int attempts = pushAttempts.merge(userId, 1, Integer::sum);
            if (attempts <= failuresBeforeSuccess) throw new Exception("push provider unavailable (attempt " + attempts + ")");
            System.out.println("  [PUSH to " + userId + "] " + message);
        };
    }

    static boolean trySend(Sender sender, String userId, String message, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sender.send(userId, message);
                return true;
            } catch (Exception e) {
                System.out.println("  Attempt " + attempt + " failed: " + e.getMessage());
            }
        }
        return false;
    }

    static void notify(String userId, String message, List<Channel> fallbackOrder, Map<Channel, Sender> senders, int maxAttemptsPerChannel) {
        for (Channel channel : fallbackOrder) {
            if (trySend(senders.get(channel), userId, message, maxAttemptsPerChannel)) {
                System.out.println("Delivered via " + channel);
                return;
            }
            System.out.println(channel + " exhausted retries, falling back");
        }
        System.out.println("All channels failed");
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        System.out.println("== Simple Notification Interview Demo ==");

        Map<Channel, Sender> senders = new HashMap<>();
        senders.put(Channel.PUSH, flakyPush(2));
        senders.put(Channel.EMAIL, (userId, message) -> System.out.println("  [EMAIL to " + userId + "] " + message));

        System.out.println("Notifying alice, preferring push (fails twice, then falls back to email since we only allow 1 attempt per channel):");
        notify("alice", "Order shipped!", Arrays.asList(Channel.PUSH, Channel.EMAIL), senders, 1);

        System.out.println("\nNotifying bob, preferring push with 3 attempts allowed (should succeed on the 3rd try):");
        notify("bob", "Password reset", Arrays.asList(Channel.PUSH, Channel.EMAIL), senders, 3);
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
