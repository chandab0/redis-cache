import React, { useState } from 'react';
import { ChevronDown, ChevronUp, ExternalLink, HelpCircle } from 'lucide-react';
import { PeopleAlsoAskItem } from '../types';

interface PeopleAlsoAskProps {
  items: PeopleAlsoAskItem[];
}

export const PeopleAlsoAsk: React.FC<PeopleAlsoAskProps> = ({ items }) => {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  if (!items || items.length === 0) return null;

  const toggleItem = (index: number) => {
    setExpandedIndex(expandedIndex === index ? null : index);
  };

  return (
    <div id="people-also-ask-card" className="my-6 border border-slate-200 dark:border-slate-800 rounded-2xl bg-white dark:bg-slate-900 shadow-xs overflow-hidden max-w-2xl">
      <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center gap-2">
        <HelpCircle className="w-4 h-4 text-blue-600 dark:text-blue-400" />
        <h3 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider">
          People also ask
        </h3>
      </div>

      <div className="divide-y divide-slate-100 dark:divide-slate-800">
        {items.map((item, idx) => {
          const isExpanded = expandedIndex === idx;
          const faviconUrl = `https://www.google.com/s2/favicons?domain=${item.sourceDomain || 'google.com'}&sz=32`;

          return (
            <div key={idx} className="transition-colors">
              <button
                onClick={() => toggleItem(idx)}
                className="w-full py-3.5 px-4 flex items-center justify-between text-left hover:bg-slate-50 dark:hover:bg-slate-800/60 cursor-pointer transition-colors"
              >
                <span className="text-sm font-semibold text-slate-800 dark:text-slate-200 pr-4">
                  {item.question}
                </span>
                <span className="text-slate-400 dark:text-slate-500 flex-shrink-0">
                  {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                </span>
              </button>

              {isExpanded && (
                <div className="px-4 pb-4 pt-1 bg-slate-50/50 dark:bg-slate-950/40 text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed border-t border-slate-100 dark:border-slate-850">
                  <p className="mb-3">{item.answer}</p>

                  {item.sourceUrl && (
                    <div className="flex items-center gap-2 pt-2 border-t border-slate-200/70 dark:border-slate-800">
                      <img
                        src={faviconUrl}
                        alt=""
                        className="w-3.5 h-3.5 object-contain rounded-xs"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                      <a
                        href={item.sourceUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-xs text-blue-700 dark:text-blue-400 font-semibold hover:underline inline-flex items-center gap-1 truncate max-w-[80%]"
                      >
                        <span>{item.sourceTitle || item.sourceDomain || 'Search Source'}</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
