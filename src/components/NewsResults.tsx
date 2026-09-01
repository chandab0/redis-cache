import React from 'react';
import { Newspaper, ExternalLink, Clock } from 'lucide-react';
import { NewsResultItem } from '../types';

interface NewsResultsProps {
  news: NewsResultItem[];
}

export const NewsResults: React.FC<NewsResultsProps> = ({ news }) => {
  if (!news || news.length === 0) {
    return (
      <div className="py-16 text-center text-gray-500 dark:text-gray-400">
        <Newspaper className="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-2" />
        <p className="text-base font-medium">No recent news stories found for this search.</p>
      </div>
    );
  }

  return (
    <div id="news-results-list" className="space-y-4 max-w-3xl">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {news.map((item) => (
          <a
            key={item.id || item.url}
            href={item.url}
            target="_blank"
            rel="noopener noreferrer"
            className="group bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-blue-400 dark:hover:border-blue-500/60 rounded-2xl p-5 shadow-2xs hover:shadow-md transition-all flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between gap-2 mb-2.5">
                <span className="text-xs font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-red-500 inline-block" />
                  {item.source}
                </span>
                <span className="text-[11px] font-medium text-slate-400 dark:text-slate-400 flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {item.publishedTime || 'Recent'}
                </span>
              </div>

              <h3 className="text-base font-bold text-slate-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors leading-snug line-clamp-2">
                {item.title}
              </h3>

              <p className="text-xs text-slate-600 dark:text-slate-400 mt-2.5 line-clamp-3 leading-relaxed">
                {item.snippet}
              </p>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-blue-600 dark:text-blue-400 font-semibold">
              <span>Read Full Coverage</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </div>
          </a>
        ))}
      </div>
    </div>
  );
};
