package socialmedia.service;

import socialmedia.model.Comment;
import socialmedia.model.Post;
import socialmedia.observer.SocialEvent;
import socialmedia.observer.SocialEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPostService implements PostService {
    private final Map<String, Post> posts = new ConcurrentHashMap<>();
    private final SocialEventPublisher publisher;
    private final AtomicLong idCounter = new AtomicLong(1);

    public InMemoryPostService(SocialEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Post createPost(String authorId, String content) {
        Post post = new Post("POST-" + idCounter.getAndIncrement(), authorId, content);
        posts.put(post.getId(), post);
        return post;
    }

    // Publish AFTER the state change has committed, outside the Post's
    // monitor -- listeners must never run while we hold a domain lock
    // (a slow listener would otherwise stall every like on that post).
    @Override
    public void likePost(String postId, String userId) {
        Post post = requirePost(postId);
        post.like(userId);
        publisher.publish(new SocialEvent(SocialEvent.Type.LIKE, userId, post.getAuthorId(), postId));
    }

    @Override
    public void addComment(String postId, String authorId, String content) {
        Post post = requirePost(postId);
        post.addComment(new Comment("COMMENT-" + idCounter.getAndIncrement(), authorId, content));
        publisher.publish(new SocialEvent(SocialEvent.Type.COMMENT, authorId, post.getAuthorId(), postId));
    }

    @Override
    public List<Post> getPostsByAuthors(List<String> authorIds) {
        Set<String> authorSet = Set.copyOf(authorIds);
        List<Post> result = new ArrayList<>();
        for (Post post : posts.values()) {
            if (authorSet.contains(post.getAuthorId())) result.add(post);
        }
        return result;
    }

    @Override
    public Optional<Post> getPost(String postId) {
        return Optional.ofNullable(posts.get(postId));
    }

    private Post requirePost(String postId) {
        Post post = posts.get(postId);
        if (post == null) throw new IllegalArgumentException("Unknown post: " + postId);
        return post;
    }
}
