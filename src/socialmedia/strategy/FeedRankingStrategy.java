package socialmedia.strategy;

import socialmedia.model.Post;

import java.util.List;

public interface FeedRankingStrategy {
    List<Post> rank(List<Post> candidatePosts);
}
