// Thin wrapper around the StoryStream REST API.
const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message ?? `Request failed (${res.status})`);
  return body;
}

export const api = {
  browse: ({ query = '', genre = '', page = 0, size = 9, sort = 'title', direction = 'asc' }) => {
    const params = new URLSearchParams({ page, size, sort, direction });
    if (query) params.set('query', query);
    if (genre) params.set('genre', genre);
    return request(`/audiobooks?${params}`);
  },
  similar: (id) => request(`/audiobooks/${id}/similar?limit=3`),
  bookmarks: (user) => request(`/users/${user}/bookmarks`),
  saveBookmark: (user, id, positionSeconds) =>
    request(`/users/${user}/bookmarks/${id}`, { method: 'PUT', body: JSON.stringify({ positionSeconds }) }),
  deleteBookmark: (user, id) => request(`/users/${user}/bookmarks/${id}`, { method: 'DELETE' }),
  history: (user) => request(`/users/${user}/history`),
  listen: (user, id, secondsListened) =>
    request(`/users/${user}/history/${id}`, { method: 'POST', body: JSON.stringify({ secondsListened }) }),
  recommendations: (user) => request(`/users/${user}/recommendations?limit=4`),
};
