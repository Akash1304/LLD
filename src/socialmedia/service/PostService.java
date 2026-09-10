package socialmedia.service;

import socialmedia.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostService {
    Post createPost(String authorId, String content);
    void likePost(String postId, String userId);
    void addComment(String postId, String authorId, String content);
    List<Post> getPostsByAuthors(List<String> authorIds);
    Optional<Post> getPost(String postId);
}
