import React, { useState, useEffect, useRef } from 'react';
import {
  Search,
  Mic,
  Camera,
  X,
  Grid,
  Bookmark,
  Moon,
  Sun,
  Settings,
  TrendingUp,
  CloudSun,
  Calculator,
  Coins,
  Dices,
  RefreshCw,
  Sparkles,
  ArrowRight,
  ExternalLink,
} from 'lucide-react';
import { GoogleLogo } from './GoogleLogo';
import { AppsLauncher } from './AppsLauncher';
import { fetchSuggestions, fetchTrendingTopics } from '../services/api';
import { TrendingItem } from '../types';

interface HomeViewProps {
  onSearch: (query: string) => void;
  onOpenVoice: () => void;
  onOpenLens: () => void;
  onOpenSettings: () => void;
  onOpenBookmarks: () => void;
  bookmarkCount: number;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
}

export const HomeView: React.FC<HomeViewProps> = ({
  onSearch,
  onOpenVoice,
  onOpenLens,
  onOpenSettings,
  onOpenBookmarks,
  bookmarkCount,
  theme,
  onToggleTheme,
}) => {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [showApps, setShowApps] = useState(false);
  const [trending, setTrending] = useState<TrendingItem[]>([]);
  const [luckyTextIndex, setLuckyTextIndex] = useState(0);

  const searchInputRef = useRef<HTMLInputElement>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const luckyPhrases = [
    "I'm Feeling Lucky",
    "I'm Feeling Curious",
    "I'm Feeling Adventurous",
    "I'm Feeling Playful",
    "I'm Feeling Artistic",
    "I'm Feeling Stellar",
  ];

  useEffect(() => {
    fetchTrendingTopics().then(setTrending);
  }, []);

  useEffect(() => {
    if (!query.trim()) {
      setSuggestions([]);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      fetchSuggestions(query, controller.signal).then(setSuggestions);
    }, 150);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

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

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      onSearch(query.trim());
    }
  };

  const handleLuckyClick = () => {
    const luckyTopics = [
      'James Webb Space Telescope latest findings',
      'Artificial intelligence milestones 2026',
      'Quantum computing advancements',
      'Renaissance art masterpieces',
      'Great Barrier Reef marine biology',
      'History of the internet and web browsers',
      'do a barrel roll',
    ];
    const picked = luckyTopics[Math.floor(Math.random() * luckyTopics.length)];
    onSearch(picked);
  };

  return (
    <div id="home-view-container" className="min-h-screen flex flex-col justify-between bg-slate-50/40 dark:bg-[#0b0f19] text-slate-900 dark:text-slate-100 transition-colors">
      {/* Top Navigation */}
      <header className="p-4 sm:px-8 sm:py-5 flex items-center justify-between">
        <div className="flex items-center gap-5 text-xs sm:text-sm font-medium text-slate-600 dark:text-slate-400">
          <button
            onClick={() => onSearch('about Google search engine')}
            className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer"
          >
            About
          </button>
          <button
            onClick={() => onSearch('Google store products')}
            className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer hidden sm:inline"
          >
            Store
          </button>
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          <button
            onClick={() => onSearch('Gmail webmail')}
            className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 font-medium transition-colors cursor-pointer hidden sm:inline px-2 py-1"
          >
            Gmail
          </button>
          <button
            onClick={() => onSearch('Google images')}
            className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 font-medium transition-colors cursor-pointer hidden sm:inline px-2 py-1"
          >
            Images
          </button>

          {/* Bookmarks */}
          <button
            onClick={onOpenBookmarks}
            className="p-2.5 text-slate-600 dark:text-slate-300 hover:bg-slate-200/60 dark:hover:bg-slate-800 rounded-xl transition-all relative cursor-pointer"
            title="Saved Bookmarks"
          >
            <Bookmark className="w-4.5 h-4.5" />
            {bookmarkCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-blue-600 text-white rounded-full text-[10px] font-bold flex items-center justify-center shadow-xs">
                {bookmarkCount}
              </span>
            )}
          </button>

          {/* Theme Switch */}
          <button
            onClick={onToggleTheme}
            className="p-2.5 text-slate-600 dark:text-slate-300 hover:bg-slate-200/60 dark:hover:bg-slate-800 rounded-xl transition-all cursor-pointer"
            title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
          >
            {theme === 'light' ? <Moon className="w-4.5 h-4.5" /> : <Sun className="w-4.5 h-4.5 text-amber-400" />}
          </button>

          {/* Settings */}
          <button
            onClick={onOpenSettings}
            className="p-2.5 text-slate-600 dark:text-slate-300 hover:bg-slate-200/60 dark:hover:bg-slate-800 rounded-xl transition-all cursor-pointer"
            title="Settings"
          >
            <Settings className="w-4.5 h-4.5" />
          </button>

          {/* Apps Launcher */}
          <div className="relative">
            <button
              onClick={() => setShowApps(!showApps)}
              className="p-2.5 text-slate-600 dark:text-slate-300 hover:bg-slate-200/60 dark:hover:bg-slate-800 rounded-xl transition-all cursor-pointer"
              title="Google Apps"
            >
              <Grid className="w-4.5 h-4.5" />
            </button>
            <AppsLauncher
              isOpen={showApps}
              onClose={() => setShowApps(false)}
              onSelectApp={(q) => onSearch(q)}
            />
          </div>

          {/* Avatar Profile */}
          <div
            onClick={onOpenSettings}
            className="w-8.5 h-8.5 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-xs cursor-pointer select-none ring-2 ring-white dark:ring-slate-800"
            title="Profile"
          >
            G
          </div>
        </div>
      </header>

      {/* Main Search Center */}
      <main className="flex-1 flex flex-col items-center justify-center px-4 max-w-4xl mx-auto w-full py-6 sm:py-10">
        {/* Logo */}
        <div className="mb-8 text-center">
          <GoogleLogo size="xl" animate />
          <div className="inline-flex items-center gap-2 mt-3 px-3 py-1 rounded-full bg-slate-100 dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700/60 text-xs font-medium text-slate-600 dark:text-slate-300">
            <Sparkles className="w-3.5 h-3.5 text-blue-500" />
            <span>AI Overviews • Live Grounding • Interactive Engine</span>
          </div>
        </div>

        {/* Search Box */}
        <div ref={wrapperRef} className="w-full max-w-2xl relative mb-6">
          <form
            onSubmit={handleSearchSubmit}
            className="flex items-center w-full bg-white dark:bg-slate-900 border border-slate-300/80 dark:border-slate-700 hover:border-slate-400 dark:hover:border-slate-600 focus-within:border-blue-600 dark:focus-within:border-blue-500 focus-within:ring-4 focus-within:ring-blue-500/10 shadow-xs hover:shadow-md focus-within:shadow-lg rounded-2xl px-4.5 py-3.5 transition-all group"
          >
            <Search className="w-5 h-5 text-slate-400 mr-3 flex-shrink-0 group-focus-within:text-blue-600 dark:group-focus-within:text-blue-400 transition-colors" />

            <input
              ref={searchInputRef}
              type="text"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setShowSuggestions(true);
              }}
              onFocus={() => setShowSuggestions(true)}
              placeholder="Search anything with AI overview and real-time web grounding..."
              className="flex-1 bg-transparent text-sm sm:text-base text-slate-900 dark:text-white focus:outline-hidden placeholder:text-slate-400 font-medium"
            />

            {query && (
              <button
                type="button"
                onClick={() => {
                  setQuery('');
                  searchInputRef.current?.focus();
                }}
                className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer mr-2 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            )}

            <div className="flex items-center gap-1.5 border-l border-slate-200 dark:border-slate-700 pl-3">
              <button
                type="button"
                onClick={onOpenVoice}
                className="p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-xl transition-colors cursor-pointer"
                title="Search by voice"
              >
                <Mic className="w-4.5 h-4.5" />
              </button>

              <button
                type="button"
                onClick={onOpenLens}
                className="p-2 text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 rounded-xl transition-colors cursor-pointer"
                title="Search by image (Lens)"
              >
                <Camera className="w-4.5 h-4.5" />
              </button>
            </div>
          </form>

          {/* Autocomplete suggestions */}
          {showSuggestions && suggestions.length > 0 && (
            <div className="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-800 overflow-hidden z-50 py-2 animate-in fade-in zoom-in-98 duration-100">
              {suggestions.map((item, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    setQuery(item);
                    setShowSuggestions(false);
                    onSearch(item);
                  }}
                  className="w-full px-5 py-2.5 text-left text-sm font-medium text-slate-800 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800/80 flex items-center gap-3 cursor-pointer transition-colors"
                >
                  <Search className="w-4 h-4 text-slate-400" />
                  <span className="truncate">{item}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-3 mb-8">
          <button
            onClick={() => query.trim() && onSearch(query.trim())}
            className="px-5 py-2.5 bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700 text-sm font-semibold text-slate-800 dark:text-slate-200 rounded-xl shadow-2xs hover:shadow-xs transition-all cursor-pointer"
          >
            Google Search
          </button>

          <button
            onClick={handleLuckyClick}
            onMouseEnter={() => setLuckyTextIndex((prev) => (prev + 1) % luckyPhrases.length)}
            className="px-5 py-2.5 bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700 text-sm font-semibold text-slate-800 dark:text-slate-200 rounded-xl shadow-2xs hover:shadow-xs transition-all cursor-pointer min-w-[170px] text-center"
          >
            {luckyPhrases[luckyTextIndex]}
          </button>
        </div>

        {/* Quick Tools & Easter Eggs Shortcuts */}
        <div className="w-full max-w-2xl mb-8">
          <div className="flex flex-wrap items-center justify-center gap-2 text-xs">
            <span className="text-slate-400 font-medium mr-1 text-[11px] uppercase tracking-wider">Quick Tools:</span>
            {[
              { label: 'Weather', icon: <CloudSun className="w-3.5 h-3.5 text-amber-500" />, q: 'weather today' },
              { label: 'Calculator', icon: <Calculator className="w-3.5 h-3.5 text-emerald-500" />, q: 'calculator' },
              { label: 'USD to EUR', icon: <RefreshCw className="w-3.5 h-3.5 text-blue-500" />, q: '100 USD to EUR' },
              { label: 'Flip Coin', icon: <Coins className="w-3.5 h-3.5 text-yellow-500" />, q: 'flip a coin' },
              { label: 'Roll Dice', icon: <Dices className="w-3.5 h-3.5 text-purple-500" />, q: 'roll a die' },
              { label: 'Barrel Roll 🌀', icon: <Sparkles className="w-3.5 h-3.5 text-pink-500" />, q: 'do a barrel roll' },
            ].map((tool, idx) => (
              <button
                key={idx}
                onClick={() => onSearch(tool.q)}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-white dark:bg-slate-850 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full text-slate-700 dark:text-slate-300 font-medium transition-all cursor-pointer border border-slate-200/80 dark:border-slate-800 shadow-2xs hover:border-blue-400 dark:hover:border-blue-500"
              >
                {tool.icon}
                <span>{tool.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Trending / Discover Section */}
        {trending && trending.length > 0 && (
          <div className="w-full max-w-3xl mt-2 pt-6 border-t border-slate-200/80 dark:border-slate-800">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="p-1 rounded-md bg-rose-100 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400">
                  <TrendingUp className="w-3.5 h-3.5" />
                </div>
                <h3 className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider">
                  Trending Searches & Discover Feed
                </h3>
              </div>
              <span className="text-[11px] text-slate-400 font-medium">Live Global Trends</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {trending.map((item, idx) => (
                <div
                  key={idx}
                  onClick={() => onSearch(item.query)}
                  className="p-3.5 rounded-xl bg-white dark:bg-slate-900 hover:bg-blue-50/40 dark:hover:bg-slate-850 border border-slate-200/80 dark:border-slate-800 hover:border-blue-300 dark:hover:border-blue-900/60 shadow-2xs hover:shadow-xs transition-all cursor-pointer group flex items-start justify-between gap-3"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-[10px] font-bold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 px-2 py-0.5 rounded-md border border-blue-100 dark:border-blue-900/40">
                        {item.category}
                      </span>
                      <span className="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                        {item.growth}
                      </span>
                    </div>

                    <h4 className="text-sm font-semibold text-slate-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors truncate">
                      {item.query}
                    </h4>

                    {item.snippet && (
                      <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-1 mt-0.5">
                        {item.snippet}
                      </p>
                    )}
                  </div>

                  <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 group-hover:translate-x-1 transition-all flex-shrink-0 mt-1" />
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white/80 dark:bg-[#070a13] border-t border-slate-200 dark:border-slate-850 text-xs text-slate-600 dark:text-slate-400">
        <div className="max-w-7xl mx-auto px-6 py-2.5 border-b border-slate-100 dark:border-slate-800/80 flex items-center justify-between text-[11px]">
          <span>United States • From your IP address • Location active</span>
          <span className="text-slate-400">Gemini Grounded Search Engine</span>
        </div>
        <div className="max-w-7xl mx-auto px-6 py-3 flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-wrap gap-5">
            <button onClick={() => onSearch('about Google search engine')} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">About</button>
            <button onClick={() => onSearch('Google advertising solutions')} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">Advertising</button>
            <button onClick={() => onSearch('Google business solutions')} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">Business</button>
            <button onClick={() => onSearch('how search engine algorithms work')} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">How Search works</button>
          </div>

          <div className="flex flex-wrap gap-5">
            <button onClick={() => onSearch('online privacy and safety')} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">Privacy</button>
            <button onClick={() => onSearch('search engine terms of service')} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">Terms</button>
            <button onClick={onOpenSettings} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors cursor-pointer">Settings</button>
          </div>
        </div>
      </footer>
    </div>
  );
};
