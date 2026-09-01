export type SearchTab = 'all' | 'images' | 'news' | 'videos' | 'finance' | 'tech';

export type TimeFilter = 'all' | 'h' | 'd' | 'w' | 'm' | 'y';

export interface GroundingSource {
  title: string;
  url: string;
  domain: string;
  snippet?: string;
  favicon?: string;
}

export interface SearchResultItem {
  id: string;
  title: string;
  url: string;
  displayUrl: string;
  domain: string;
  snippet: string;
  date?: string;
  sitelinks?: { title: string; url: string; snippet?: string }[];
  isBookmarked?: boolean;
}

export interface KnowledgePanelData {
  title: string;
  subtitle?: string;
  description: string;
  imageUrl?: string;
  attributes: { label: string; value: string }[];
  websiteUrl?: string;
  wikipediaUrl?: string;
  socialLinks?: { platform: string; url: string }[];
  relatedEntities?: { name: string; type: string; imageUrl?: string }[];
}

export interface PeopleAlsoAskItem {
  question: string;
  answer: string;
  sourceTitle?: string;
  sourceUrl?: string;
  sourceDomain?: string;
}

export interface ImageResultItem {
  id: string;
  title: string;
  imageUrl: string;
  sourceUrl: string;
  sourceDomain: string;
  width?: number;
  height?: number;
}

export interface NewsResultItem {
  id: string;
  title: string;
  snippet: string;
  source: string;
  url: string;
  publishedTime: string;
  imageUrl?: string;
}

export interface VideoResultItem {
  id: string;
  title: string;
  snippet: string;
  source: string;
  channel: string;
  url: string;
  duration?: string;
  views?: string;
  thumbnailUrl?: string;
}

export type WidgetType = 'calculator' | 'weather' | 'converter' | 'dictionary' | 'time' | 'finance' | 'easter_egg' | 'none';

export interface InteractiveWidgetData {
  type: WidgetType;
  title?: string;
  data?: any;
}

export interface SearchResponse {
  query: string;
  correctedQuery?: string;
  searchTimeSeconds: number;
  totalEstimatedResults: number;
  aiOverview?: {
    summary: string;
    keyTakeaways?: string[];
    sources: GroundingSource[];
  };
  widget?: InteractiveWidgetData;
  results: SearchResultItem[];
  knowledgePanel?: KnowledgePanelData;
  peopleAlsoAsk: PeopleAlsoAskItem[];
  images: ImageResultItem[];
  news: NewsResultItem[];
  videos: VideoResultItem[];
  relatedSearches: string[];
}

export interface TrendingItem {
  query: string;
  category: string;
  growth: string;
  snippet?: string;
}

export interface HistoryItem {
  id: string;
  query: string;
  timestamp: number;
  tab?: SearchTab;
}

export interface BookmarkItem {
  id: string;
  title: string;
  url: string;
  domain: string;
  snippet: string;
  dateAdded: number;
}

export interface SearchSettings {
  safeSearch: 'strict' | 'moderate' | 'off';
  region: string;
  language: string;
  resultsPerPage: number;
  openInNewTab: boolean;
  theme: 'light' | 'dark' | 'system';
}
