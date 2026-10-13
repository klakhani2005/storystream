package com.portfolio.storystream.repository;

import com.portfolio.storystream.model.ListeningHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListeningHistoryRepository extends JpaRepository<ListeningHistory, Long> {

    // Fetch the audiobook too: callers read its title after the transaction ends.
    @EntityGraph(attributePaths = "audiobook")
    Optional<ListeningHistory> findByUserIdAndAudiobookId(String userId, Long audiobookId);

    @EntityGraph(attributePaths = "audiobook")
    List<ListeningHistory> findByUserIdOrderByUpdatedAtDesc(String userId);
}
