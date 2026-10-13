package com.portfolio.storystream.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * A normal B-tree index can't speed up "contains" searches (LIKE '%term%').
 * On PostgreSQL we add trigram GIN indexes on lower(title) and lower(author),
 * which can. Skipped on other databases (e.g. H2 in tests).
 */
@Component
public class SearchIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexInitializer.class);

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public SearchIndexInitializer(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createSearchIndexes() throws Exception {
        String product;
        try (var conn = dataSource.getConnection()) {
            product = conn.getMetaData().getDatabaseProductName();
        }
        if (!"PostgreSQL".equalsIgnoreCase(product)) {
            log.info("Skipping trigram indexes on {}", product);
            return;
        }
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_audiobook_title_trgm "
                + "ON audiobooks USING gin (lower(title) gin_trgm_ops)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_audiobook_author_trgm "
                + "ON audiobooks USING gin (lower(author) gin_trgm_ops)");
        log.info("Trigram search indexes ready");
    }
}
