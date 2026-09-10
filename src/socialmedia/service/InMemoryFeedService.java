package socialmedia.service;

import socialmedia.model.Post;
import socialmedia.model.User;
import socialmedia.strategy.FeedRankingStrategy;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFeedService implements FeedService {
    private final UserService userService;
    private final PostService postService;

    public InMemoryFeedService(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    @Override
    public List<Post> getFeed(String userId, FeedRankingStrategy rankingStrategy) {
        User user = userService.getUser(userId).orElseThrow(() -> new IllegalArgumentException("Unknown user: " + userId));
        List<String> following = new ArrayList<>(user.getFollowingIds());
        List<Post> candidates = postService.getPostsByAuthors(following);
        return rankingStrategy.rank(candidates);
    }
}
