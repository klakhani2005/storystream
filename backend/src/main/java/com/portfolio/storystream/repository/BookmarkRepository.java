package com.portfolio.storystream.repository;

import com.portfolio.storystream.model.Bookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // Fetch the audiobook too: callers read its title after the transaction ends.
    @EntityGraph(attributePaths = "audiobook")
    Optional<Bookmark> findByUserIdAndAudiobookId(String userId, Long audiobookId);

    @EntityGraph(attributePaths = "audiobook")
    List<Bookmark> findByUserIdOrderByUpdatedAtDesc(String userId);
}
