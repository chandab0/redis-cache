import React from 'react';

interface FooterProps {
  onSearch: (q: string) => void;
  onOpenSettings: () => void;
}

export const Footer: React.FC<FooterProps> = ({ onSearch, onOpenSettings }) => {
  return (
    <footer className="mt-16 bg-slate-100/90 dark:bg-slate-950 border-t border-slate-200 dark:border-slate-800/90 text-xs text-slate-500 dark:text-slate-400">
      <div className="max-w-7xl mx-auto px-6 py-3 border-b border-slate-200/80 dark:border-slate-850 flex items-center justify-between">
        <span className="font-medium">United States • AI-Enhanced Live Web Index</span>
        <span className="text-[11px] font-semibold text-blue-600 dark:text-blue-400">Gemini Grounded Search</span>
      </div>
      <div className="max-w-7xl mx-auto px-6 py-3.5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap gap-4 sm:gap-6 font-medium">
          <button onClick={() => onSearch('Help with Google search')} className="hover:text-slate-900 dark:hover:text-slate-200 cursor-pointer transition-colors">Help</button>
          <button onClick={() => onSearch('Send search feedback')} className="hover:text-slate-900 dark:hover:text-slate-200 cursor-pointer transition-colors">Send feedback</button>
          <button onClick={() => onSearch('Privacy policy overview')} className="hover:text-slate-900 dark:hover:text-slate-200 cursor-pointer transition-colors">Privacy</button>
          <button onClick={() => onSearch('Terms of service')} className="hover:text-slate-900 dark:hover:text-slate-200 cursor-pointer transition-colors">Terms</button>
        </div>

        <div className="flex flex-wrap gap-4 sm:gap-6 font-medium">
          <button onClick={onOpenSettings} className="hover:text-slate-900 dark:hover:text-slate-200 cursor-pointer transition-colors">Search settings</button>
          <button onClick={() => onSearch('Advanced search options')} className="hover:text-slate-900 dark:hover:text-slate-200 cursor-pointer transition-colors">Advanced search</button>
        </div>
      </div>
    </footer>
  );
};
