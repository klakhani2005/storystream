import React, { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';
import BookCard from './components/BookCard.jsx';
import LibraryPanel from './components/LibraryPanel.jsx';
import Pagination from './components/Pagination.jsx';

const USER = 'demo-user'; // demo identity, not authentication
const GENRES = ['', 'Science Fiction', 'Memoir', 'Personal Development', 'Productivity'];

export default function App() {
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState({ query: '', genre: '', page: 0, sort: 'title', direction: 'asc' });
  const [results, setResults] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [library, setLibrary] = useState({ bookmarks: [], history: [], recommendations: [] });
  const [similar, setSimilar] = useState(null);
  const [message, setMessage] = useState('');

  const loadCatalog = useCallback(async () => {
    try {
      setResults(await api.browse(filters));
      setMessage('');
    } catch (e) {
      setMessage(`Cannot load the catalog: ${e.message}. Is the Spring Boot API running on port 8080?`);
    }
  }, [filters]);

  const loadLibrary = useCallback(async () => {
    try {
      const [bookmarks, history, recommendations] = await Promise.all([
        api.bookmarks(USER), api.history(USER), api.recommendations(USER),
      ]);
      setLibrary({ bookmarks, history, recommendations });
    } catch {
      /* catalog error message already covers a down API */
    }
  }, []);

  useEffect(() => { loadCatalog(); }, [loadCatalog]);
  useEffect(() => { loadLibrary(); }, [loadLibrary]);

  const run = async (action, success) => {
    try {
      await action();
      setMessage(success);
      loadLibrary();
    } catch (e) {
      setMessage(e.message);
    }
  };

  return (
    <main className="page">
      <header>
        <div className="mark">SS</div>
        <div>
          <p className="eyebrow">YOUR NEXT LISTEN</p>
          <h1>StoryStream</h1>
        </div>
      </header>

      <form className="search" onSubmit={(e) => { e.preventDefault(); setFilters({ ...filters, query, page: 0 }); }}>
        <input value={query} onChange={(e) => setQuery(e.target.value)}
               placeholder="Search by title or author…" aria-label="Search audiobooks" />
        <select aria-label="Genre" value={filters.genre}
                onChange={(e) => setFilters({ ...filters, genre: e.target.value, query: '', page: 0 })}>
          {GENRES.map((g) => <option key={g} value={g}>{g || 'All genres'}</option>)}
        </select>
        <select aria-label="Sort" value={`${filters.sort}:${filters.direction}`}
                onChange={(e) => { const [sort, direction] = e.target.value.split(':'); setFilters({ ...filters, sort, direction, page: 0 }); }}>
          <option value="title:asc">Title A–Z</option>
          <option value="rating:desc">Top rated</option>
          <option value="durationMinutes:asc">Shortest</option>
        </select>
        <button>Search</button>
        <button type="button" className="secondary"
                onClick={() => { setQuery(''); setFilters({ query: '', genre: '', page: 0, sort: 'title', direction: 'asc' }); }}>
          Clear
        </button>
      </form>

      {message && <p className="notice" role="status">{message}</p>}

      <div className="layout">
        <section>
          <p className="muted">{results.totalElements} audiobooks</p>
          <div className="catalog">
            {results.content.map((book) => (
              <BookCard key={book.id} book={book}
                onListen={(b) => run(() => api.listen(USER, b.id, 900), `Logged 15 minutes of “${b.title}”.`)}
                onBookmark={(b) => run(() => api.saveBookmark(USER, b.id, 0), `Bookmarked “${b.title}”.`)}
                onSimilar={async (b) => setSimilar({ title: b.title, books: await api.similar(b.id) })}
              />
            ))}
          </div>
          <Pagination page={results.page} totalPages={results.totalPages}
                      onChange={(page) => setFilters({ ...filters, page })} />
          {similar && (
            <div className="similar">
              <h3>More like “{similar.title}”</h3>
              {similar.books.length === 0 ? <p className="empty">No similar titles yet.</p>
                : similar.books.map((b) => <p key={b.id}>{b.title} <span className="muted">by {b.author}</span></p>)}
            </div>
          )}
        </section>
        <LibraryPanel {...library}
          onRemoveBookmark={(id) => run(() => api.deleteBookmark(USER, id), 'Bookmark removed.')} />
      </div>
      <footer>StoryStream portfolio demo · Sample catalog data</footer>
    </main>
  );
}
