package socialmedia.service;

import socialmedia.model.Post;
import socialmedia.strategy.FeedRankingStrategy;

import java.util.List;

public interface FeedService {
    List<Post> getFeed(String userId, FeedRankingStrategy rankingStrategy);
}
