import React, { useState, useEffect } from 'react';
import { X, Bookmark, ExternalLink, Trash2, Search, Download } from 'lucide-react';
import { BookmarkItem } from '../types';
import { getBookmarks, toggleBookmark } from '../services/api';

interface BookmarksModalProps {
  isOpen: boolean;
  onClose: () => void;
  onBookmarkChanged?: () => void;
}

export const BookmarksModal: React.FC<BookmarksModalProps> = ({
  isOpen,
  onClose,
  onBookmarkChanged,
}) => {
  const [bookmarks, setBookmarks] = useState<BookmarkItem[]>([]);
  const [filterQuery, setFilterQuery] = useState<string>('');

  useEffect(() => {
    if (isOpen) {
      setBookmarks(getBookmarks());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleRemove = (bm: BookmarkItem) => {
    toggleBookmark(bm);
    const updated = getBookmarks();
    setBookmarks(updated);
    if (onBookmarkChanged) onBookmarkChanged();
  };

  const handleExport = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(bookmarks, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', 'google_search_saved_bookmarks.json');
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const filteredBookmarks = bookmarks.filter(
    (b) =>
      b.title.toLowerCase().includes(filterQuery.toLowerCase()) ||
      b.domain.toLowerCase().includes(filterQuery.toLowerCase()) ||
      b.snippet.toLowerCase().includes(filterQuery.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-2xl w-full overflow-hidden shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-4 px-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400">
              <Bookmark className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">
                Saved Search Results ({bookmarks.length})
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Starred results and bookmarks saved from your searches
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            {bookmarks.length > 0 && (
              <button
                onClick={handleExport}
                className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
                title="Export Bookmarks as JSON"
              >
                <Download className="w-4 h-4" />
              </button>
            )}
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Search in bookmarks */}
        {bookmarks.length > 0 && (
          <div className="p-4 px-6 border-b border-slate-100 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-950">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Filter saved results..."
                value={filterQuery}
                onChange={(e) => setFilterQuery(e.target.value)}
                className="w-full text-xs pl-9 pr-4 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
              />
            </div>
          </div>
        )}

        {/* List */}
        <div className="p-6 overflow-y-auto flex-1 space-y-3">
          {filteredBookmarks.length === 0 ? (
            <div className="py-16 text-center text-slate-400">
              <Bookmark className="w-10 h-10 mx-auto mb-2 opacity-40 text-blue-500" />
              <p className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                {bookmarks.length === 0 ? 'No bookmarks saved yet' : 'No matches found'}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Click the bookmark icon on any search result to save it for future reference.
              </p>
            </div>
          ) : (
            filteredBookmarks.map((bm) => {
              const faviconUrl = `https://www.google.com/s2/favicons?domain=${bm.domain || 'google.com'}&sz=32`;
              return (
                <div
                  key={bm.id || bm.url}
                  className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-950 hover:bg-blue-50/40 dark:hover:bg-slate-800/80 border border-slate-200/80 dark:border-slate-800 transition-all flex items-start justify-between gap-3 group"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <img
                        src={faviconUrl}
                        alt=""
                        className="w-4 h-4 object-contain rounded-xs"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                      <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 truncate">
                        {bm.domain}
                      </span>
                    </div>

                    <h4 className="text-sm font-semibold text-blue-700 dark:text-blue-400 hover:underline truncate">
                      <a href={bm.url} target="_blank" rel="noopener noreferrer">
                        {bm.title}
                      </a>
                    </h4>

                    <p className="text-xs text-slate-600 dark:text-slate-400 line-clamp-2 mt-1 leading-relaxed">
                      {bm.snippet}
                    </p>
                  </div>

                  <div className="flex items-center gap-1">
                    <a
                      href={bm.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="p-2 text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-white dark:hover:bg-slate-800 rounded-lg transition-colors"
                      title="Open page"
                    >
                      <ExternalLink className="w-4 h-4" />
                    </a>
                    <button
                      onClick={() => handleRemove(bm)}
                      className="p-2 text-slate-400 hover:text-red-500 hover:bg-white dark:hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                      title="Remove bookmark"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
