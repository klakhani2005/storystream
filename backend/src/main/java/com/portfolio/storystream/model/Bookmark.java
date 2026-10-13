package com.portfolio.storystream.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** A saved position in an audiobook. One bookmark per (user, audiobook); saving again moves it. */
@Entity
@Table(name = "bookmarks",
        uniqueConstraints = @UniqueConstraint(name = "uq_bookmark_user_book", columnNames = {"user_id", "audiobook_id"}))
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audiobook_id", nullable = false)
    private Audiobook audiobook;

    @Column(nullable = false)
    private int positionSeconds;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Bookmark() {
    }

    public Bookmark(String userId, Audiobook audiobook, int positionSeconds) {
        this.userId = userId;
        this.audiobook = audiobook;
        moveTo(positionSeconds);
    }

    public void moveTo(int positionSeconds) {
        this.positionSeconds = positionSeconds;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Audiobook getAudiobook() { return audiobook; }
    public int getPositionSeconds() { return positionSeconds; }
    public Instant getUpdatedAt() { return updatedAt; }
}
