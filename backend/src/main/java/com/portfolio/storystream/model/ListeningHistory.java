package com.portfolio.storystream.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Total time a user has listened to one audiobook. Each listening session adds to it. */
@Entity
@Table(name = "listening_history",
        uniqueConstraints = @UniqueConstraint(name = "uq_history_user_book", columnNames = {"user_id", "audiobook_id"}),
        indexes = @Index(name = "idx_history_user_updated", columnList = "user_id, updated_at"))
public class ListeningHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audiobook_id", nullable = false)
    private Audiobook audiobook;

    @Column(nullable = false)
    private int secondsListened;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ListeningHistory() {
    }

    public ListeningHistory(String userId, Audiobook audiobook) {
        this.userId = userId;
        this.audiobook = audiobook;
        this.updatedAt = Instant.now();
    }

    public void addListening(int seconds) {
        this.secondsListened += seconds;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Audiobook getAudiobook() { return audiobook; }
    public int getSecondsListened() { return secondsListened; }
    public Instant getUpdatedAt() { return updatedAt; }
}
