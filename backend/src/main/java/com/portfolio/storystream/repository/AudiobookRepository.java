package com.portfolio.storystream.repository;

import com.portfolio.storystream.model.Audiobook;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AudiobookRepository extends JpaRepository<Audiobook, Long> {

    /**
     * Case-insensitive substring search on title or author.
     * `pattern` is already lower-cased, escaped and wrapped in % by CatalogService.
     * On PostgreSQL this is served by the trigram GIN indexes on lower(title)/lower(author).
     */
    @Query("""
            select a from Audiobook a
            where lower(a.title) like :pattern escape '\\'
               or lower(a.author) like :pattern escape '\\'
            """)
    Page<Audiobook> search(@Param("pattern") String pattern, Pageable pageable);

    Page<Audiobook> findByGenreIgnoreCase(String genre, Pageable pageable);

    /** Same genre or same author, best rated first, excluding the given ids. */
    @Query("""
            select a from Audiobook a
            where (lower(a.genre) = lower(:genre) or a.author = :author)
              and a.id not in :excludeIds
            order by a.rating desc, a.id asc
            """)
    List<Audiobook> findSimilar(@Param("genre") String genre, @Param("author") String author,
                                @Param("excludeIds") Collection<Long> excludeIds, Pageable limit);

    /** Best-rated books in any of the given genres, excluding the given ids. */
    @Query("""
            select a from Audiobook a
            where lower(a.genre) in :genres and a.id not in :excludeIds
            order by a.rating desc, a.id asc
            """)
    List<Audiobook> findTopInGenres(@Param("genres") Collection<String> genres,
                                    @Param("excludeIds") Collection<Long> excludeIds, Pageable limit);
}
