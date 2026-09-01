import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, AlertCircle, Sparkles } from 'lucide-react';
import {
  SearchTab,
  TimeFilter,
  SearchResponse,
  SearchResultItem,
  SearchSettings,
} from './types';
import {
  executeSearch,
  getBookmarks,
  toggleBookmark,
  getStoredSettings,
} from './services/api';
import { HomeView } from './components/HomeView';
import { Header } from './components/Header';
import { AIOverview } from './components/AIOverview';
import { KnowledgePanel } from './components/KnowledgePanel';
import { OrganicResults } from './components/OrganicResults';
import { PeopleAlsoAsk } from './components/PeopleAlsoAsk';
import { ImageResults } from './components/ImageResults';
import { NewsResults } from './components/NewsResults';
import { VideoResults } from './components/VideoResults';
import { RelatedSearches } from './components/RelatedSearches';
import { Pagination } from './components/Pagination';
import { Footer } from './components/Footer';
import { CalculatorWidget } from './components/InteractiveWidgets/CalculatorWidget';
import { WeatherWidget } from './components/InteractiveWidgets/WeatherWidget';
import { UnitConverterWidget } from './components/InteractiveWidgets/UnitConverterWidget';
import { DictionaryWidget } from './components/InteractiveWidgets/DictionaryWidget';
import { TimeWidget } from './components/InteractiveWidgets/TimeWidget';
import { FinanceWidget } from './components/InteractiveWidgets/FinanceWidget';
import { EasterEggBanner } from './components/InteractiveWidgets/EasterEggBanner';
import { LensModal } from './components/LensModal';
import { VoiceSearchModal } from './components/VoiceSearchModal';
import { SettingsModal } from './components/SettingsModal';
import { BookmarksModal } from './components/BookmarksModal';

export default function App() {
  const [view, setView] = useState<'home' | 'results'>('home');
  const [currentQuery, setCurrentQuery] = useState<string>('');
  const [activeTab, setActiveTab] = useState<SearchTab>('all');
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('all');
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [searchResponse, setSearchResponse] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Settings & Theme
  const [settings, setSettings] = useState<SearchSettings>(getStoredSettings());
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const stored = getStoredSettings();
    if (stored.theme === 'dark') return 'dark';
    if (stored.theme === 'light') return 'light';
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  // Bookmarks
  const [bookmarkedUrls, setBookmarkedUrls] = useState<Set<string>>(
    () => new Set(getBookmarks().map((b) => b.url))
  );

  // Modals
  const [isVoiceOpen, setIsVoiceOpen] = useState(false);
  const [isLensOpen, setIsLensOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isBookmarksOpen, setIsBookmarksOpen] = useState(false);

  // Sync theme to root DOM element
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  const handleToggleTheme = () => {
    const next: 'light' | 'dark' = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    const updatedSettings: SearchSettings = { ...settings, theme: next };
    setSettings(updatedSettings);
  };

  const reloadBookmarks = useCallback(() => {
    setBookmarkedUrls(new Set(getBookmarks().map((b) => b.url)));
  }, []);

  const handleToggleBookmark = (item: SearchResultItem) => {
    toggleBookmark({
      title: item.title,
      url: item.url,
      domain: item.domain,
      snippet: item.snippet,
    });
    reloadBookmarks();
  };

  const handleExecuteSearch = async (
    query: string,
    tab: SearchTab = 'all',
    time: TimeFilter = 'all',
    page: number = 1
  ) => {
    if (!query.trim()) return;

    setCurrentQuery(query.trim());
    setActiveTab(tab);
    setTimeFilter(time);
    setCurrentPage(page);
    setView('results');
    setLoading(true);
    setError(null);

    try {
      const data = await executeSearch(query.trim(), tab, time, settings);
      setSearchResponse(data);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err: any) {
      setError(err?.message || 'Failed to retrieve search results. Please check connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (newTab: SearchTab) => {
    setActiveTab(newTab);
    handleExecuteSearch(currentQuery, newTab, timeFilter, 1);
  };

  const handleTimeFilterChange = (newTime: TimeFilter) => {
    setTimeFilter(newTime);
    handleExecuteSearch(currentQuery, activeTab, newTime, 1);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    handleExecuteSearch(currentQuery, activeTab, timeFilter, page);
  };

  const handleGoHome = () => {
    setView('home');
    setCurrentQuery('');
    setSearchResponse(null);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-slate-50/40 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors flex flex-col justify-between selection:bg-blue-100 dark:selection:bg-blue-900/50 font-sans">
      {view === 'home' ? (
        <HomeView
          onSearch={(q) => handleExecuteSearch(q, 'all')}
          onOpenVoice={() => setIsVoiceOpen(true)}
          onOpenLens={() => setIsLensOpen(true)}
          onOpenSettings={() => setIsSettingsOpen(true)}
          onOpenBookmarks={() => setIsBookmarksOpen(true)}
          bookmarkCount={bookmarkedUrls.size}
          theme={theme}
          onToggleTheme={handleToggleTheme}
        />
      ) : (
        <div className="flex-1 flex flex-col">
          {/* SERP Header */}
          <Header
            query={currentQuery}
            onSearch={(q, tab, time) => handleExecuteSearch(q, tab || activeTab, time || timeFilter, 1)}
            activeTab={activeTab}
            onTabChange={handleTabChange}
            timeFilter={timeFilter}
            onTimeFilterChange={handleTimeFilterChange}
            onOpenVoice={() => setIsVoiceOpen(true)}
            onOpenLens={() => setIsLensOpen(true)}
            onOpenSettings={() => setIsSettingsOpen(true)}
            onOpenBookmarks={() => setIsBookmarksOpen(true)}
            bookmarkCount={bookmarkedUrls.size}
            theme={theme}
            onToggleTheme={handleToggleTheme}
            onGoHome={handleGoHome}
          />

          {/* Main Results Container */}
          <main className="max-w-7xl mx-auto px-4 sm:px-6 py-4 flex-1 w-full">
            {/* Search Statistics & Query Correction */}
            {!loading && searchResponse && (
              <div className="text-xs text-slate-500 dark:text-slate-400 mb-4 flex items-center justify-between border-b border-slate-200/60 dark:border-slate-800/80 pb-2">
                <span>
                  About {searchResponse.totalEstimatedResults?.toLocaleString() || '1,200,000'} results ({searchResponse.searchTimeSeconds || '0.28'} seconds)
                </span>
                {searchResponse.correctedQuery && (
                  <div className="text-xs text-red-600 dark:text-red-400 font-medium">
                    Did you mean:{' '}
                    <button
                      onClick={() => handleExecuteSearch(searchResponse.correctedQuery!)}
                      className="underline font-bold hover:text-red-700 cursor-pointer"
                    >
                      {searchResponse.correctedQuery}
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Loading Skeleton */}
            {loading && (
              <div className="py-8 space-y-6 max-w-2xl animate-pulse">
                {/* AI Overview Skeleton */}
                <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/70 dark:border-slate-800 space-y-3 shadow-2xs">
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-lg bg-slate-200 dark:bg-slate-800" />
                    <div className="w-28 h-4 rounded-lg bg-slate-200 dark:bg-slate-800" />
                  </div>
                  <div className="w-full h-3 rounded-lg bg-slate-200 dark:bg-slate-800" />
                  <div className="w-5/6 h-3 rounded-lg bg-slate-200 dark:bg-slate-800" />
                  <div className="w-3/4 h-3 rounded-lg bg-slate-200 dark:bg-slate-800" />
                </div>

                {/* Results Skeletons */}
                {[1, 2, 3, 4].map((i) => (
                  <div key={i} className="space-y-2 p-4 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/70 dark:border-slate-800 shadow-2xs">
                    <div className="w-40 h-3 rounded-lg bg-slate-200 dark:bg-slate-800" />
                    <div className="w-80 h-5 rounded-lg bg-slate-200 dark:bg-slate-800" />
                    <div className="w-full h-3.5 rounded-lg bg-slate-200 dark:bg-slate-800" />
                    <div className="w-4/5 h-3.5 rounded-lg bg-slate-200 dark:bg-slate-800" />
                  </div>
                ))}
              </div>
            )}

            {/* Error Message */}
            {error && !loading && (
              <div className="my-8 p-6 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-2xl max-w-xl text-center mx-auto shadow-2xs">
                <AlertCircle className="w-10 h-10 mx-auto text-red-500 mb-2" />
                <h3 className="text-base font-bold text-red-800 dark:text-red-300">
                  Search Encountered An Error
                </h3>
                <p className="text-xs text-red-600 dark:text-red-400 mt-1">{error}</p>
                <button
                  onClick={() => handleExecuteSearch(currentQuery, activeTab, timeFilter, currentPage)}
                  className="mt-4 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-semibold cursor-pointer"
                >
                  Retry Search
                </button>
              </div>
            )}

            {/* Main Content Layout based on activeTab */}
            {!loading && searchResponse && (
              <>
                {/* ALL TAB (Standard SERP with AI Overview, Widgets, Results, Knowledge Panel) */}
                {activeTab === 'all' && (
                  <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
                    {/* Left Column: AI Overview, Widgets, Organic Results, PAA */}
                    <div className="lg:col-span-8 space-y-6">
                      {/* Interactive Widget if present */}
                      {searchResponse.widget && (
                        <div>
                          {searchResponse.widget.type === 'calculator' && (
                            <CalculatorWidget
                              initialExpression={searchResponse.widget.data?.expression}
                              initialResult={searchResponse.widget.data?.result}
                            />
                          )}
                          {searchResponse.widget.type === 'weather' && (
                            <WeatherWidget data={searchResponse.widget.data} />
                          )}
                          {searchResponse.widget.type === 'converter' && (
                            <UnitConverterWidget data={searchResponse.widget.data} />
                          )}
                          {searchResponse.widget.type === 'dictionary' && (
                            <DictionaryWidget data={searchResponse.widget.data} />
                          )}
                          {searchResponse.widget.type === 'time' && (
                            <TimeWidget data={searchResponse.widget.data} />
                          )}
                          {searchResponse.widget.type === 'finance' && (
                            <FinanceWidget data={searchResponse.widget.data} />
                          )}
                          {searchResponse.widget.type === 'easter_egg' && (
                            <EasterEggBanner effect={searchResponse.widget.data?.effect} />
                          )}
                        </div>
                      )}

                      {/* AI Overview Box */}
                      {searchResponse.aiOverview?.summary && (
                        <AIOverview
                          summary={searchResponse.aiOverview.summary}
                          keyTakeaways={searchResponse.aiOverview.keyTakeaways}
                          sources={searchResponse.aiOverview.sources || []}
                          query={currentQuery}
                        />
                      )}

                      {/* People Also Ask (PAA) */}
                      {searchResponse.peopleAlsoAsk && searchResponse.peopleAlsoAsk.length > 0 && (
                        <PeopleAlsoAsk items={searchResponse.peopleAlsoAsk} />
                      )}

                      {/* Organic Search Results */}
                      <OrganicResults
                        results={searchResponse.results}
                        bookmarkedUrls={bookmarkedUrls}
                        onToggleBookmark={handleToggleBookmark}
                        query={currentQuery}
                      />

                      {/* Related Searches */}
                      {searchResponse.relatedSearches && searchResponse.relatedSearches.length > 0 && (
                        <RelatedSearches
                          searches={searchResponse.relatedSearches}
                          onSearchClick={(q) => handleExecuteSearch(q, 'all')}
                        />
                      )}

                      {/* Pagination */}
                      <Pagination
                        currentPage={currentPage}
                        onPageChange={handlePageChange}
                      />
                    </div>

                    {/* Right Column: Knowledge Graph Panel */}
                    <div className="lg:col-span-4">
                      {searchResponse.knowledgePanel && (
                        <KnowledgePanel
                          data={searchResponse.knowledgePanel}
                          onRelatedClick={(q) => handleExecuteSearch(q, 'all')}
                        />
                      )}
                    </div>
                  </div>
                )}

                {/* IMAGES TAB */}
                {activeTab === 'images' && (
                  <ImageResults images={searchResponse.images} />
                )}

                {/* NEWS TAB */}
                {activeTab === 'news' && (
                  <NewsResults news={searchResponse.news} />
                )}

                {/* VIDEOS TAB */}
                {activeTab === 'videos' && (
                  <VideoResults videos={searchResponse.videos} />
                )}

                {/* FINANCE TAB */}
                {activeTab === 'finance' && (
                  <div className="max-w-2xl space-y-6">
                    <FinanceWidget data={searchResponse.widget?.data} />
                    <OrganicResults
                      results={searchResponse.results}
                      bookmarkedUrls={bookmarkedUrls}
                      onToggleBookmark={handleToggleBookmark}
                      query={currentQuery}
                    />
                  </div>
                )}

                {/* TECH & CODE TAB */}
                {activeTab === 'tech' && (
                  <div className="max-w-2xl space-y-6">
                    {searchResponse.aiOverview?.summary && (
                      <AIOverview
                        summary={searchResponse.aiOverview.summary}
                        keyTakeaways={searchResponse.aiOverview.keyTakeaways}
                        sources={searchResponse.aiOverview.sources || []}
                        query={currentQuery}
                      />
                    )}
                    <OrganicResults
                      results={searchResponse.results}
                      bookmarkedUrls={bookmarkedUrls}
                      onToggleBookmark={handleToggleBookmark}
                      query={currentQuery}
                    />
                  </div>
                )}
              </>
            )}
          </main>

          {/* Footer */}
          <Footer
            onSearch={(q) => handleExecuteSearch(q, 'all')}
            onOpenSettings={() => setIsSettingsOpen(true)}
          />
        </div>
      )}

      {/* Interactive Modals */}
      <LensModal
        isOpen={isLensOpen}
        onClose={() => setIsLensOpen(false)}
        onSelectQuery={(q) => handleExecuteSearch(q, 'all')}
      />

      <VoiceSearchModal
        isOpen={isVoiceOpen}
        onClose={() => setIsVoiceOpen(false)}
        onTranscriptReady={(transcript) => handleExecuteSearch(transcript, 'all')}
      />

      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        settings={settings}
        onUpdateSettings={(newSettings) => {
          setSettings(newSettings);
          setTheme(newSettings.theme === 'system' ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light') : newSettings.theme);
        }}
        onSelectQuery={(q) => handleExecuteSearch(q, 'all')}
      />

      <BookmarksModal
        isOpen={isBookmarksOpen}
        onClose={() => setIsBookmarksOpen(false)}
        onBookmarkChanged={reloadBookmarks}
      />
    </div>
  );
}
