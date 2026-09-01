import React, { useState, useEffect, useRef } from 'react';
import {
  Search,
  Mic,
  Camera,
  X,
  SlidersHorizontal,
  Grid,
  Bookmark,
  Moon,
  Sun,
  Settings,
  Image as ImageIcon,
  Newspaper,
  Video,
  DollarSign,
  Cpu,
  Clock,
  Sparkles,
} from 'lucide-react';
import { GoogleLogo } from './GoogleLogo';
import { SearchTab, TimeFilter } from '../types';
import { fetchSuggestions } from '../services/api';
import { AppsLauncher } from './AppsLauncher';

interface HeaderProps {
  query: string;
  onSearch: (q: string, tab?: SearchTab, time?: TimeFilter) => void;
  activeTab: SearchTab;
  onTabChange: (tab: SearchTab) => void;
  timeFilter: TimeFilter;
  onTimeFilterChange: (time: TimeFilter) => void;
  onOpenVoice: () => void;
  onOpenLens: () => void;
  onOpenSettings: () => void;
  onOpenBookmarks: () => void;
  bookmarkCount: number;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onGoHome: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  query,
  onSearch,
  activeTab,
  onTabChange,
  timeFilter,
  onTimeFilterChange,
  onOpenVoice,
  onOpenLens,
  onOpenSettings,
  onOpenBookmarks,
  bookmarkCount,
  theme,
  onToggleTheme,
  onGoHome,
}) => {
  const [inputValue, setInputValue] = useState(query);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [showTools, setShowTools] = useState(false);
  const [showApps, setShowApps] = useState(false);

  const searchInputRef = useRef<HTMLInputElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setInputValue(query);
  }, [query]);

  useEffect(() => {
    if (!inputValue.trim()) {
      setSuggestions([]);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      fetchSuggestions(inputValue, controller.signal).then(setSuggestions);
    }, 150);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [inputValue]);

  // Click outside suggestions
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputValue.trim()) {
      setShowSuggestions(false);
      onSearch(inputValue.trim(), activeTab, timeFilter);
    }
  };

  const handleSuggestionClick = (item: string) => {
    setInputValue(item);
    setShowSuggestions(false);
    onSearch(item, activeTab, timeFilter);
  };

  const tabs: { id: SearchTab; label: string; icon: React.ReactNode }[] = [
    { id: 'all', label: 'All', icon: <Search className="w-3.5 h-3.5" /> },
    { id: 'images', label: 'Images', icon: <ImageIcon className="w-3.5 h-3.5" /> },
    { id: 'news', label: 'News', icon: <Newspaper className="w-3.5 h-3.5" /> },
    { id: 'videos', label: 'Videos', icon: <Video className="w-3.5 h-3.5" /> },
    { id: 'finance', label: 'Finance', icon: <DollarSign className="w-3.5 h-3.5" /> },
    { id: 'tech', label: 'Tech & Code', icon: <Cpu className="w-3.5 h-3.5" /> },
  ];

  return (
    <header className="sticky top-0 z-30 bg-white/95 dark:bg-[#0b0f19]/95 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-3 pb-1">
        {/* Top row */}
        <div className="flex items-center justify-between gap-4">
          {/* Left: Logo & Search Box */}
          <div className="flex items-center gap-4 md:gap-8 flex-1 max-w-3xl">
            <GoogleLogo size="sm" onClick={onGoHome} className="flex-shrink-0" />

            <div ref={wrapperRef} className="relative flex-1 max-w-2xl">
              <form
                onSubmit={handleSubmit}
                className="flex items-center w-full bg-slate-50/80 dark:bg-slate-900/90 hover:bg-white dark:hover:bg-slate-900 border border-slate-200 dark:border-slate-700/80 hover:border-slate-300 dark:hover:border-slate-600 focus-within:bg-white dark:focus-within:bg-slate-900 focus-within:border-blue-600 dark:focus-within:border-blue-500 focus-within:ring-3 focus-within:ring-blue-500/10 shadow-2xs hover:shadow-xs focus-within:shadow-md rounded-xl px-3.5 py-2 transition-all"
              >
                <input
                  ref={searchInputRef}
                  type="text"
                  value={inputValue}
                  onChange={(e) => {
                    setInputValue(e.target.value);
                    setShowSuggestions(true);
                  }}
                  onFocus={() => setShowSuggestions(true)}
                  placeholder="Search the web with Gemini AI..."
                  className="flex-1 bg-transparent text-sm text-slate-900 dark:text-white focus:outline-hidden pr-2 placeholder:text-slate-400 font-medium"
                />

                {inputValue && (
                  <button
                    type="button"
                    onClick={() => {
                      setInputValue('');
                      searchInputRef.current?.focus();
                    }}
                    className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-md hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer mr-1 transition-colors"
                  >
                    <X className="w-4 h-4" />
                  </button>
                )}

                <div className="flex items-center gap-1 border-l border-slate-200 dark:border-slate-700 pl-2">
                  <button
                    type="button"
                    onClick={onOpenVoice}
                    className="p-1.5 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-lg transition-colors cursor-pointer"
                    title="Search by voice"
                  >
                    <Mic className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={onOpenLens}
                    className="p-1.5 text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 rounded-lg transition-colors cursor-pointer"
                    title="Search by image (Lens)"
                  >
                    <Camera className="w-4 h-4" />
                  </button>

                  <button
                    type="submit"
                    className="p-1.5 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-lg transition-colors cursor-pointer"
                    title="Search"
                  >
                    <Search className="w-4 h-4" />
                  </button>
                </div>
              </form>

              {/* Suggestions Dropdown */}
              {showSuggestions && suggestions.length > 0 && (
                <div className="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-slate-900 rounded-xl shadow-xl border border-slate-200 dark:border-slate-800 overflow-hidden z-50 py-1.5 animate-in fade-in duration-100">
                  {suggestions.map((item, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSuggestionClick(item)}
                      className="w-full px-4 py-2 text-left text-xs md:text-sm font-medium text-slate-800 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 flex items-center gap-3 cursor-pointer transition-colors"
                    >
                      <Search className="w-3.5 h-3.5 text-slate-400" />
                      <span className="truncate">{item}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right Controls */}
          <div className="flex items-center gap-1 sm:gap-2">
            {/* Bookmarks */}
            <button
              onClick={onOpenBookmarks}
              className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors relative cursor-pointer"
              title="Saved Bookmarks"
            >
              <Bookmark className="w-4.5 h-4.5" />
              {bookmarkCount > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 bg-blue-600 text-white rounded-full text-[10px] font-bold flex items-center justify-center shadow-2xs">
                  {bookmarkCount}
                </span>
              )}
            </button>

            {/* Theme Toggle */}
            <button
              onClick={onToggleTheme}
              className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors cursor-pointer"
              title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
            >
              {theme === 'light' ? <Moon className="w-4.5 h-4.5" /> : <Sun className="w-4.5 h-4.5 text-amber-400" />}
            </button>

            {/* Settings */}
            <button
              onClick={onOpenSettings}
              className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors cursor-pointer"
              title="Search Settings"
            >
              <Settings className="w-4.5 h-4.5" />
            </button>

            {/* Apps Launcher (9 dots) */}
            <div className="relative">
              <button
                onClick={() => setShowApps(!showApps)}
                className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors cursor-pointer"
                title="Google Apps"
              >
                <Grid className="w-4.5 h-4.5" />
              </button>
              <AppsLauncher
                isOpen={showApps}
                onClose={() => setShowApps(false)}
                onSelectApp={(q) => onSearch(q, 'all')}
              />
            </div>

            {/* User Profile Avatar */}
            <div
              onClick={onOpenSettings}
              className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-2xs cursor-pointer select-none ring-2 ring-white dark:ring-slate-850"
              title="Account Profile"
            >
              G
            </div>
          </div>
        </div>

        {/* Category Tabs row */}
        <div className="flex items-center justify-between border-t border-slate-100 dark:border-slate-800/80 mt-2.5 pt-1.5 overflow-x-auto scrollbar-none">
          <div className="flex items-center gap-1 sm:gap-1.5">
            {tabs.map((t) => {
              const isSelected = activeTab === t.id;
              return (
                <button
                  key={t.id}
                  onClick={() => onTabChange(t.id)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800/80 shadow-2xs'
                      : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  {t.icon}
                  <span>{t.label}</span>
                </button>
              );
            })}
          </div>

          {/* Tools Filter Button */}
          <div className="relative">
            <button
              onClick={() => setShowTools(!showTools)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                showTools || timeFilter !== 'all'
                  ? 'bg-slate-200 dark:bg-slate-700 text-slate-900 dark:text-white'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <SlidersHorizontal className="w-3.5 h-3.5" />
              <span>Tools</span>
              {timeFilter !== 'all' && (
                <span className="w-1.5 h-1.5 rounded-full bg-blue-600" />
              )}
            </button>

            {/* Tools Dropdown */}
            {showTools && (
              <div className="absolute right-0 top-full mt-2 bg-white dark:bg-slate-900 rounded-xl shadow-xl border border-slate-200 dark:border-slate-800 p-2.5 z-40 w-52 animate-in fade-in zoom-in-95 duration-100">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1.5 px-2">
                  Time Filter
                </span>
                <div className="space-y-0.5">
                  {[
                    { id: 'all', label: 'Any time' },
                    { id: 'h', label: 'Past hour' },
                    { id: 'd', label: 'Past 24 hours' },
                    { id: 'w', label: 'Past week' },
                    { id: 'm', label: 'Past month' },
                    { id: 'y', label: 'Past year' },
                  ].map((filter) => (
                    <button
                      key={filter.id}
                      onClick={() => {
                        onTimeFilterChange(filter.id as TimeFilter);
                        setShowTools(false);
                      }}
                      className={`w-full text-left px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer flex items-center justify-between ${
                        timeFilter === filter.id
                          ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 font-bold'
                          : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                      }`}
                    >
                      <span>{filter.label}</span>
                      {timeFilter === filter.id && <span className="text-blue-600 text-xs font-bold">✓</span>}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
