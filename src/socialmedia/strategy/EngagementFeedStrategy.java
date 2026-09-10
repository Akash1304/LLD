package socialmedia.strategy;

import socialmedia.model.Post;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EngagementFeedStrategy implements FeedRankingStrategy {
    @Override
    public List<Post> rank(List<Post> candidatePosts) {
        return candidatePosts.stream()
                .sorted(Comparator.comparingInt(Post::getEngagementScore).reversed()
                        .thenComparing(Comparator.comparing(Post::getCreatedAt).reversed()))
                .collect(Collectors.toList());
    }
}
