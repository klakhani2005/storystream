import React from 'react';
import { formatDuration } from './BookCard.jsx';

export default function LibraryPanel({ bookmarks, history, recommendations, onRemoveBookmark }) {
  return (
    <aside className="library">
      <section>
        <h3>Continue listening</h3>
        {history.length === 0 && <p className="empty">Nothing yet. Press “Listen” on a book.</p>}
        {history.map((h) => (
          <div className="row" key={h.audiobookId}>
            <span>{h.title}</span>
            <div className="progress" aria-label={`${h.percentComplete}% complete`}>
              <div style={{ width: `${h.percentComplete}%` }} />
            </div>
          </div>
        ))}
      </section>
      <section>
        <h3>Bookmarks</h3>
        {bookmarks.length === 0 && <p className="empty">No bookmarks saved.</p>}
        {bookmarks.map((b) => (
          <div className="row" key={b.audiobookId}>
            <span>{b.title} · {formatDuration(Math.floor(b.positionSeconds / 60))}</span>
            <button className="link" onClick={() => onRemoveBookmark(b.audiobookId)}>Remove</button>
          </div>
        ))}
      </section>
      <section>
        <h3>Recommended for you</h3>
        {recommendations.map((r) => (
          <div className="row" key={r.id}>
            <span>{r.title}</span>
            <span className="muted">★ {r.rating.toFixed(1)}</span>
          </div>
        ))}
      </section>
    </aside>
  );
}
