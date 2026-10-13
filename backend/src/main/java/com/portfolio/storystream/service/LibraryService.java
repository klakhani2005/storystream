package com.portfolio.storystream.service;

import com.portfolio.storystream.model.Audiobook;
import com.portfolio.storystream.model.Bookmark;
import com.portfolio.storystream.model.ListeningHistory;
import com.portfolio.storystream.repository.BookmarkRepository;
import com.portfolio.storystream.repository.ListeningHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-user bookmarks and listening history, persisted in PostgreSQL. */
@Service
@Transactional
public class LibraryService {

    private final CatalogService catalog;
    private final BookmarkRepository bookmarks;
    private final ListeningHistoryRepository history;

    public LibraryService(CatalogService catalog, BookmarkRepository bookmarks, ListeningHistoryRepository history) {
        this.catalog = catalog;
        this.bookmarks = bookmarks;
        this.history = history;
    }

    /** Creates the bookmark, or moves the existing one for this user and book. */
    public Bookmark saveBookmark(String userId, Long audiobookId, int positionSeconds) {
        Audiobook book = catalog.get(audiobookId);
        if (positionSeconds > book.getDurationMinutes() * 60) {
            throw new IllegalArgumentException("positionSeconds is past the end of the audiobook");
        }
        return bookmarks.findByUserIdAndAudiobookId(userId, audiobookId)
                .map(existing -> {
                    existing.moveTo(positionSeconds);
                    return existing;
                })
                .orElseGet(() -> bookmarks.save(new Bookmark(userId, book, positionSeconds)));
    }

    @Transactional(readOnly = true)
    public List<Bookmark> bookmarksFor(String userId) {
        return bookmarks.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public void deleteBookmark(String userId, Long audiobookId) {
        Bookmark b = bookmarks.findByUserIdAndAudiobookId(userId, audiobookId)
                .orElseThrow(() -> new NotFoundException("No bookmark for audiobook " + audiobookId));
        bookmarks.delete(b);
    }

    /** Adds a listening session to the user's running total for this book. */
    public ListeningHistory recordListening(String userId, Long audiobookId, int secondsListened) {
        Audiobook book = catalog.get(audiobookId);
        ListeningHistory entry = history.findByUserIdAndAudiobookId(userId, audiobookId)
                .orElseGet(() -> history.save(new ListeningHistory(userId, book)));
        entry.addListening(secondsListened);
        return entry;
    }

    @Transactional(readOnly = true)
    public List<ListeningHistory> historyFor(String userId) {
        return history.findByUserIdOrderByUpdatedAtDesc(userId);
    }
}
