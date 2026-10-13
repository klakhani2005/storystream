package com.portfolio.storystream.web;

import com.portfolio.storystream.service.CatalogService;
import com.portfolio.storystream.service.LibraryService;
import com.portfolio.storystream.web.Dtos.AudiobookResponse;
import com.portfolio.storystream.web.Dtos.BookmarkResponse;
import com.portfolio.storystream.web.Dtos.HistoryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bookmarks, listening history and recommendations for a user.
 * NOTE: userId is a plain identifier for this demo, not authentication.
 */
@RestController
@RequestMapping("/api/users/{userId}")
@Validated
public class LibraryController {

    private static final String USER_ID = Dtos.USER_ID_PATTERN;

    private final LibraryService library;
    private final CatalogService catalog;

    public LibraryController(LibraryService library, CatalogService catalog) {
        this.library = library;
        this.catalog = catalog;
    }

    @PutMapping("/bookmarks/{audiobookId}")
    public BookmarkResponse saveBookmark(@PathVariable @Pattern(regexp = USER_ID) String userId,
                                         @PathVariable Long audiobookId,
                                         @Valid @RequestBody PositionRequest req) {
        return BookmarkResponse.from(library.saveBookmark(userId, audiobookId, req.positionSeconds()));
    }

    @GetMapping("/bookmarks")
    public List<BookmarkResponse> bookmarks(@PathVariable @Pattern(regexp = USER_ID) String userId) {
        return library.bookmarksFor(userId).stream().map(BookmarkResponse::from).toList();
    }

    @DeleteMapping("/bookmarks/{audiobookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBookmark(@PathVariable @Pattern(regexp = USER_ID) String userId,
                               @PathVariable Long audiobookId) {
        library.deleteBookmark(userId, audiobookId);
    }

    @PostMapping("/history/{audiobookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public HistoryResponse recordListening(@PathVariable @Pattern(regexp = USER_ID) String userId,
                                           @PathVariable Long audiobookId,
                                           @Valid @RequestBody SessionRequest req) {
        return HistoryResponse.from(library.recordListening(userId, audiobookId, req.secondsListened()));
    }

    @GetMapping("/history")
    public List<HistoryResponse> history(@PathVariable @Pattern(regexp = USER_ID) String userId) {
        return library.historyFor(userId).stream().map(HistoryResponse::from).toList();
    }

    @GetMapping("/recommendations")
    public List<AudiobookResponse> recommendations(@PathVariable @Pattern(regexp = USER_ID) String userId,
                                                   @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return catalog.recommendedFor(userId, limit).stream().map(AudiobookResponse::from).toList();
    }

    public record PositionRequest(@Min(0) int positionSeconds) {
    }

    public record SessionRequest(@Min(1) @Max(86_400) int secondsListened) {
    }
}
