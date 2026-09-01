import React from 'react';
import { Bookmark, BookmarkCheck, ExternalLink, MoreVertical } from 'lucide-react';
import { SearchResultItem } from '../types';

interface OrganicResultsProps {
  results: SearchResultItem[];
  bookmarkedUrls: Set<string>;
  onToggleBookmark: (result: SearchResultItem) => void;
  query: string;
}

export const OrganicResults: React.FC<OrganicResultsProps> = ({
  results,
  bookmarkedUrls,
  onToggleBookmark,
  query,
}) => {
  if (!results || results.length === 0) {
    return (
      <div className="py-12 text-center text-gray-500 dark:text-gray-400">
        <p className="text-base font-medium">No standard web results found for this query.</p>
        <p className="text-xs mt-1">Try different keywords or check spelling.</p>
      </div>
    );
  }

  // Highlight keywords in snippets
  const highlightSnippet = (snippet: string) => {
    if (!query) return snippet;
    const words = query.toLowerCase().split(/\s+/).filter(w => w.length > 2);
    if (words.length === 0) return snippet;

    // Split words with regex safely
    const pattern = new RegExp(`(${words.map(w => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'gi');
    const parts = snippet.split(pattern);

    return parts.map((part, i) =>
      words.some(w => w.toLowerCase() === part.toLowerCase()) ? (
        <strong key={i} className="font-bold text-gray-900 dark:text-white">
          {part}
        </strong>
      ) : (
        part
      )
    );
  };

  return (
    <div id="organic-results-list" className="space-y-6 max-w-2xl">
      {results.map((result) => {
        const isBookmarked = bookmarkedUrls.has(result.url);
        const faviconUrl = `https://www.google.com/s2/favicons?domain=${result.domain || 'google.com'}&sz=32`;

        return (
          <article
            key={result.id || result.url}
            className="group transition-all"
          >
            {/* Header: Favicon + Domain Breadcrumbs + Actions */}
            <div className="flex items-center justify-between gap-2 mb-1">
              <a
                href={result.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 max-w-[85%] text-xs text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 truncate"
              >
                <div className="w-5 h-5 rounded-md bg-slate-100 dark:bg-slate-800 flex items-center justify-center p-0.5 border border-slate-200 dark:border-slate-700 flex-shrink-0">
                  <img
                    src={faviconUrl}
                    alt=""
                    className="w-3.5 h-3.5 object-contain rounded-xs"
                    onError={(e) => {
                      (e.target as HTMLElement).style.display = 'none';
                    }}
                  />
                </div>
                <div className="flex flex-col truncate">
                  <span className="font-semibold text-slate-800 dark:text-slate-200 text-xs truncate">
                    {result.domain}
                  </span>
                  <span className="text-[11px] text-slate-400 dark:text-slate-400 truncate">
                    {result.displayUrl || result.url}
                  </span>
                </div>
              </a>

              <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                <button
                  onClick={() => onToggleBookmark(result)}
                  className={`p-1.5 rounded-lg transition-colors cursor-pointer ${
                    isBookmarked
                      ? 'text-blue-600 bg-blue-50 dark:bg-blue-950/60 hover:bg-blue-100 dark:hover:bg-blue-900/60'
                      : 'text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                  title={isBookmarked ? 'Remove bookmark' : 'Bookmark result'}
                >
                  {isBookmarked ? <BookmarkCheck className="w-4 h-4" /> : <Bookmark className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Title link */}
            <h3 className="text-base sm:text-lg font-semibold tracking-tight mb-1">
              <a
                href={result.url}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-700 dark:text-blue-400 hover:underline inline-flex items-center gap-1.5 visited:text-indigo-900 dark:visited:text-indigo-300"
              >
                <span>{result.title}</span>
              </a>
            </h3>

            {/* Snippet */}
            <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
              {result.date && (
                <span className="text-slate-400 dark:text-slate-400 mr-1.5 font-medium">
                  {result.date} —
                </span>
              )}
              {highlightSnippet(result.snippet)}
            </p>

            {/* Sitelinks if present */}
            {result.sitelinks && result.sitelinks.length > 0 && (
              <div className="mt-3 pl-3.5 border-l-2 border-slate-200 dark:border-slate-800 grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {result.sitelinks.slice(0, 4).map((link, idx) => (
                  <div key={idx} className="space-y-0.5">
                    <a
                      href={link.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-xs sm:text-sm font-medium text-blue-700 dark:text-blue-400 hover:underline block truncate"
                    >
                      {link.title}
                    </a>
                    {link.snippet && (
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-2 leading-tight">
                        {link.snippet}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </article>
        );
      })}
    </div>
  );
};
