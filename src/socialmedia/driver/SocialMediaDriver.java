package socialmedia.driver;

import socialmedia.model.Post;
import socialmedia.observer.NotificationListener;
import socialmedia.observer.SocialEventPublisher;
import socialmedia.service.FeedService;
import socialmedia.service.InMemoryFeedService;
import socialmedia.service.InMemoryPostService;
import socialmedia.service.InMemoryUserService;
import socialmedia.service.PostService;
import socialmedia.service.UserService;
import socialmedia.strategy.ChronologicalFeedStrategy;
import socialmedia.strategy.EngagementFeedStrategy;

import java.util.List;

public class SocialMediaDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        // Observer: services publish, the notification listener reacts;
        // neither service knows notifications exist
        SocialEventPublisher publisher = new SocialEventPublisher();
        publisher.subscribe(new NotificationListener());

        UserService userService = new InMemoryUserService(publisher);
        PostService postService = new InMemoryPostService(publisher);
        FeedService feedService = new InMemoryFeedService(userService, postService);

        userService.createUser("alice", "Alice");
        userService.createUser("bob", "Bob");
        userService.createUser("carol", "Carol");

        System.out.println("Alice follows Bob and Carol (followees get notified):");
        userService.follow("alice", "bob");
        userService.follow("alice", "carol");

        Post bobPost1 = postService.createPost("bob", "Just shipped a new feature!");
        Post carolPost = postService.createPost("carol", "Coffee tastes better on Mondays");
        Post bobPost2 = postService.createPost("bob", "Anyone up for lunch?");

        System.out.println("\nEngagement on Carol's post (Carol gets notified of each):");
        postService.likePost(carolPost.getId(), "alice");
        postService.likePost(carolPost.getId(), "bob");
        postService.addComment(carolPost.getId(), "alice", "Agreed!");

        System.out.println("\nAlice's feed, chronological (newest first):");
        List<Post> chronoFeed = feedService.getFeed("alice", new ChronologicalFeedStrategy());
        chronoFeed.forEach(System.out::println);

        System.out.println("\nAlice's feed, ranked by engagement (Carol's popular post should rise to the top):");
        List<Post> engagementFeed = feedService.getFeed("alice", new EngagementFeedStrategy());
        engagementFeed.forEach(System.out::println);

        System.out.println("\nCarol's post detail: " + carolPost.getComments());
    }
}
