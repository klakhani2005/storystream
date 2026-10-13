import React from 'react';

export function formatDuration(minutes) {
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

export function initials(title) {
  return title.split(' ').slice(0, 2).map((w) => w[0]).join('');
}

export default function BookCard({ book, onListen, onBookmark, onSimilar }) {
  return (
    <article className="book">
      <div className="cover" aria-hidden="true">{initials(book.title)}</div>
      <div className="details">
        <span className="genre">{book.genre}</span>
        <h2>{book.title}</h2>
        <p className="author">by {book.author}</p>
        <p>{book.description}</p>
        <div className="meta">
          <span>★ {Number(book.rating).toFixed(1)}</span>
          <span>{formatDuration(book.durationMinutes)}</span>
        </div>
        <div className="actions">
          <button onClick={() => onListen(book)}>Listen 15 min</button>
          <button className="secondary" onClick={() => onBookmark(book)}>Bookmark</button>
          <button className="link" onClick={() => onSimilar(book)}>More like this</button>
        </div>
      </div>
    </article>
  );
}
