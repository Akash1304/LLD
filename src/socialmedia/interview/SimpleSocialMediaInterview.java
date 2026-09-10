package socialmedia.interview;

import java.time.Instant;
import java.util.*;

// Compact, single-file interview-friendly social media demo.
// Supports: following, posting, liking, and a feed ranked either
// chronologically or by engagement (likes + comments).
public class SimpleSocialMediaInterview {

    static class Post {
        final String author;
        final String content;
        final Instant createdAt = Instant.now();
        int likes = 0;
        int comments = 0;
        Post(String author, String content) { this.author = author; this.content = content; }
        int engagement() { return likes + comments; }
        @Override public String toString() { return author + ": \"" + content + "\" (likes=" + likes + ", comments=" + comments + ")"; }
    }

    final Map<String, Set<String>> following = new HashMap<>();
    final List<Post> posts = new ArrayList<>();

    void follow(String follower, String followee) {
        following.computeIfAbsent(follower, k -> new HashSet<>()).add(followee);
    }

    Post post(String author, String content) {
        Post p = new Post(author, content);
        posts.add(p);
        return p;
    }

    List<Post> feed(String user, boolean byEngagement) {
        Set<String> followed = following.getOrDefault(user, Set.of());
        List<Post> candidates = new ArrayList<>();
        for (Post p : posts) if (followed.contains(p.author)) candidates.add(p);
        if (byEngagement) {
            candidates.sort(Comparator.comparingInt(Post::engagement).reversed());
        } else {
            candidates.sort(Comparator.comparing((Post p) -> p.createdAt).reversed());
        }
        return candidates;
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleSocialMediaInterview social = new SimpleSocialMediaInterview();
        System.out.println("== Simple Social Media Interview Demo ==");

        social.follow("alice", "bob");
        social.follow("alice", "carol");

        social.post("bob", "Just shipped a new feature!");
        Post carolPost = social.post("carol", "Coffee tastes better on Mondays");
        social.post("bob", "Anyone up for lunch?");

        carolPost.likes = 5;
        carolPost.comments = 2;

        System.out.println("Alice's feed, chronological:");
        social.feed("alice", false).forEach(System.out::println);

        System.out.println("\nAlice's feed, by engagement (Carol's post should rise to the top):");
        social.feed("alice", true).forEach(System.out::println);
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
