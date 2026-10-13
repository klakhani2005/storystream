-- StoryStream search benchmark: how much do trigram indexes speed up catalog search?
--
-- Run against the Docker database (from the StoryStream folder):
--   docker compose exec -T db psql -U storystream -d storystream < scripts/benchmark_search.sql
--
-- Uses its own table (bench_audiobooks), so your app data is untouched.
-- The query has the same shape the app sends:
--   lower(title) LIKE $1 OR lower(author) LIKE $1  ORDER BY title  LIMIT 12
\timing off
\pset pager off
SET client_min_messages = warning;

CREATE EXTENSION IF NOT EXISTS pg_trgm;
DROP TABLE IF EXISTS bench_audiobooks;
CREATE TABLE bench_audiobooks (
    id bigserial PRIMARY KEY,
    title varchar(180) NOT NULL,
    author varchar(120) NOT NULL,
    genre varchar(80) NOT NULL,
    duration_minutes int NOT NULL,
    rating double precision NOT NULL
);

-- 200,000 synthetic books built from word lists (deterministic seed).
SELECT setseed(0.42);
INSERT INTO bench_audiobooks (title, author, genre, duration_minutes, rating)
SELECT
    initcap((ARRAY['silent','lost','hidden','broken','golden','last','dark','wild','iron','crimson',
                   'frozen','burning','secret','ancient','distant','quiet','endless','fallen','hollow','bright'])[1 + floor(random()*20)::int])
      || ' ' ||
    initcap((ARRAY['river','empire','garden','signal','kingdom','harbor','mirror','forest','machine','orbit',
                   'city','voyage','archive','planet','tower','winter','shadow','engine','island','frontier'])[1 + floor(random()*20)::int])
      || ' ' || g,
    (ARRAY['Ava','Liam','Noah','Mia','Ethan','Zoe','Omar','Priya','Kenji','Sofia'])[1 + floor(random()*10)::int]
      || ' ' ||
    (ARRAY['Carter','Nguyen','Patel','Garcia','Kim','Okafor','Rossi','Schmidt','Haddad','Lopez'])[1 + floor(random()*10)::int]
      || g % 997,
    (ARRAY['Science Fiction','Memoir','Mystery','Fantasy','History','Productivity','Romance','Thriller'])[1 + floor(random()*8)::int],
    60 + floor(random()*1200)::int,
    round((3 + random()*2)::numeric, 1)
FROM generate_series(1, 200000) AS g;
ANALYZE bench_audiobooks;

PREPARE search(text) AS
    SELECT id, title, author FROM bench_audiobooks
    WHERE lower(title) LIKE $1 OR lower(author) LIKE $1
    ORDER BY title, id
    LIMIT 12;

-- Timing helper: runs the prepared search N times and reports median ms.
CREATE OR REPLACE FUNCTION pg_temp.median_ms(pattern text, runs int) RETURNS numeric AS $$
DECLARE t0 timestamptz; samples numeric[] := '{}'; i int;
BEGIN
    FOR i IN 1..runs LOOP
        t0 := clock_timestamp();
        EXECUTE 'EXECUTE search(' || quote_literal(pattern) || ')';
        samples := samples || (extract(epoch FROM clock_timestamp() - t0) * 1000)::numeric;
    END LOOP;
    RETURN (SELECT round(percentile_cont(0.5) WITHIN GROUP (ORDER BY s)::numeric, 3) FROM unnest(samples) s);
END $$ LANGUAGE plpgsql;

CREATE TEMP TABLE results(pattern text, phase text, median_ms numeric);

-- Phase 1: no search index (sequential scan)
INSERT INTO results SELECT p, 'before', pg_temp.median_ms(p, 30)
FROM unnest(ARRAY['%harbor 1234%', '%okafor42%', '%zzz-not-found%']) p;

\echo
\echo ===== Plan WITHOUT trigram index =====
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, SUMMARY ON) EXECUTE search('%harbor 1234%');

-- Phase 2: the indexes the app creates (SearchIndexInitializer)
CREATE INDEX idx_bench_title_trgm  ON bench_audiobooks USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_bench_author_trgm ON bench_audiobooks USING gin (lower(author) gin_trgm_ops);
ANALYZE bench_audiobooks;
DEALLOCATE search;
PREPARE search(text) AS
    SELECT id, title, author FROM bench_audiobooks
    WHERE lower(title) LIKE $1 OR lower(author) LIKE $1
    ORDER BY title, id
    LIMIT 12;

INSERT INTO results SELECT p, 'after', pg_temp.median_ms(p, 30)
FROM unnest(ARRAY['%harbor 1234%', '%okafor42%', '%zzz-not-found%']) p;

\echo
\echo ===== Plan WITH trigram index =====
EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF, SUMMARY ON) EXECUTE search('%harbor 1234%');

\echo
\echo ===== Median latency over 30 runs (200,000 rows) =====
SELECT b.pattern,
       b.median_ms AS before_ms,
       a.median_ms AS after_ms,
       round(100 * (1 - a.median_ms / nullif(b.median_ms, 0)), 1) AS reduction_pct
FROM results b JOIN results a ON a.pattern = b.pattern AND a.phase = 'after'
WHERE b.phase = 'before'
ORDER BY b.pattern;

DROP TABLE bench_audiobooks;
