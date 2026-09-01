import React from 'react';
import { Search } from 'lucide-react';

interface RelatedSearchesProps {
  searches: string[];
  onSearchClick: (query: string) => void;
}

export const RelatedSearches: React.FC<RelatedSearchesProps> = ({
  searches,
  onSearchClick,
}) => {
  if (!searches || searches.length === 0) return null;

  return (
    <div id="related-searches-box" className="my-8 max-w-2xl">
      <h3 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider mb-3.5 flex items-center gap-2">
        <Search className="w-4 h-4 text-blue-600 dark:text-blue-400" />
        <span>Related searches</span>
      </h3>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
        {searches.map((item, idx) => (
          <button
            key={idx}
            onClick={() => onSearchClick(item)}
            className="flex items-center gap-3 p-3 bg-white dark:bg-slate-900 hover:bg-blue-50/60 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800 hover:border-blue-300 dark:hover:border-blue-700/60 rounded-xl text-left text-xs sm:text-sm font-medium text-slate-800 dark:text-slate-200 transition-all shadow-2xs cursor-pointer group"
          >
            <Search className="w-4 h-4 text-slate-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 flex-shrink-0 transition-colors" />
            <span className="truncate">{item}</span>
          </button>
        ))}
      </div>
    </div>
  );
};
