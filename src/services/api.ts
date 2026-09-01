import { SearchResponse, SearchTab, TimeFilter, TrendingItem, SearchSettings, BookmarkItem, HistoryItem } from '../types';

export const DEFAULT_SETTINGS: SearchSettings = {
  safeSearch: 'moderate',
  region: 'United States',
  language: 'en',
  resultsPerPage: 10,
  openInNewTab: true,
  theme: 'light',
};

const SEARCH_CACHE = new Map<string, { data: SearchResponse; timestamp: number }>();
const CACHE_TTL_MS = 1000 * 60 * 10; // 10 minutes

export async function executeSearch(
  query: string,
  tab: SearchTab = 'all',
  timeFilter: TimeFilter = 'all',
  settings: SearchSettings = DEFAULT_SETTINGS,
  signal?: AbortSignal
): Promise<SearchResponse> {
  const cacheKey = `${query.toLowerCase().trim()}_${tab}_${timeFilter}_${settings.safeSearch}`;
  const cached = SEARCH_CACHE.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    return cached.data;
  }

  const response = await fetch('/api/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query,
      tab,
      timeFilter,
      safeSearch: settings.safeSearch,
      region: settings.region,
    }),
    signal,
  });

  if (!response.ok) {
    const errData = await response.json().catch(() => ({}));
    throw new Error(errData.error || `Search failed with status ${response.status}`);
  }

  const data: SearchResponse = await response.json();
  SEARCH_CACHE.set(cacheKey, { data, timestamp: Date.now() });

  // Save to search history
  saveHistoryItem(query, tab);

  return data;
}

export async function fetchSuggestions(query: string, signal?: AbortSignal): Promise<string[]> {
  try {
    const res = await fetch(`/api/suggest?q=${encodeURIComponent(query)}`, { signal });
    if (!res.ok) return [];
    const data = await res.json();
    return data.suggestions || [];
  } catch {
    return [];
  }
}

export async function analyzeWithLens(
  imageBase64: string,
  mimeType: string,
  prompt?: string
): Promise<any> {
  const res = await fetch('/api/lens', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ imageBase64, mimeType, prompt }),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || 'Lens visual search failed');
  }

  return res.json();
}

export async function fetchTrendingTopics(): Promise<TrendingItem[]> {
  try {
    const res = await fetch('/api/trending');
    if (!res.ok) return [];
    return await res.json();
  } catch {
    return [];
  }
}

// Local Storage helpers for Search History, Bookmarks, and Settings

const HISTORY_KEY = 'google_search_history';
const BOOKMARKS_KEY = 'google_search_bookmarks';
const SETTINGS_KEY = 'google_search_settings';

export function getHistory(): HistoryItem[] {
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveHistoryItem(query: string, tab: SearchTab = 'all') {
  if (!query || !query.trim()) return;
  try {
    const history = getHistory().filter((h) => h.query.toLowerCase() !== query.toLowerCase().trim());
    const newItem: HistoryItem = {
      id: `${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      query: query.trim(),
      timestamp: Date.now(),
      tab,
    };
    const updated = [newItem, ...history].slice(0, 30);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
  } catch {
    // Ignore storage issues
  }
}

export function deleteHistoryItem(id: string) {
  try {
    const history = getHistory().filter((h) => h.id !== id);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
    return history;
  } catch {
    return [];
  }
}

export function clearAllHistory() {
  try {
    localStorage.removeItem(HISTORY_KEY);
  } catch {
    // Ignore
  }
}

export function getBookmarks(): BookmarkItem[] {
  try {
    const raw = localStorage.getItem(BOOKMARKS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function toggleBookmark(item: { title: string; url: string; domain: string; snippet: string }): boolean {
  try {
    const bookmarks = getBookmarks();
    const existingIndex = bookmarks.findIndex((b) => b.url === item.url);
    if (existingIndex >= 0) {
      bookmarks.splice(existingIndex, 1);
      localStorage.setItem(BOOKMARKS_KEY, JSON.stringify(bookmarks));
      return false; // Removed
    } else {
      const newBookmark: BookmarkItem = {
        id: `bm_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
        title: item.title,
        url: item.url,
        domain: item.domain,
        snippet: item.snippet,
        dateAdded: Date.now(),
      };
      bookmarks.unshift(newBookmark);
      localStorage.setItem(BOOKMARKS_KEY, JSON.stringify(bookmarks));
      return true; // Added
    }
  } catch {
    return false;
  }
}

export function isItemBookmarked(url: string): boolean {
  const bookmarks = getBookmarks();
  return bookmarks.some((b) => b.url === url);
}

export function getStoredSettings(): SearchSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    return raw ? { ...DEFAULT_SETTINGS, ...JSON.parse(raw) } : DEFAULT_SETTINGS;
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveStoredSettings(settings: SearchSettings) {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch {
    // Ignore
  }
}
