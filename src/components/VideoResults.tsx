import React from 'react';
import { Play, Video as VideoIcon, ExternalLink } from 'lucide-react';
import { VideoResultItem } from '../types';

interface VideoResultsProps {
  videos: VideoResultItem[];
}

export const VideoResults: React.FC<VideoResultsProps> = ({ videos }) => {
  if (!videos || videos.length === 0) {
    return (
      <div className="py-16 text-center text-gray-500 dark:text-gray-400">
        <VideoIcon className="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-2" />
        <p className="text-base font-medium">No video results found for this search.</p>
      </div>
    );
  }

  return (
    <div id="video-results-list" className="space-y-4 max-w-2xl">
      {videos.map((vid) => {
        const thumb = vid.thumbnailUrl || 'https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=500&auto=format&fit=crop&q=60';
        return (
          <a
            key={vid.id || vid.url}
            href={vid.url}
            target="_blank"
            rel="noopener noreferrer"
            className="group flex flex-col sm:flex-row gap-4 p-3.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl hover:border-blue-400 dark:hover:border-blue-500/60 transition-all shadow-2xs hover:shadow-xs"
          >
            {/* Thumbnail */}
            <div className="relative sm:w-48 aspect-16/9 rounded-xl overflow-hidden bg-slate-950 flex-shrink-0">
              <img
                src={thumb}
                alt={vid.title}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300 opacity-90"
                onError={(e) => {
                  (e.target as HTMLImageElement).src =
                    'https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=500&auto=format&fit=crop&q=60';
                }}
              />
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-10 h-10 rounded-full bg-slate-950/70 backdrop-blur-xs flex items-center justify-center text-white group-hover:scale-110 group-hover:bg-red-600 transition-all shadow-md">
                  <Play className="w-4 h-4 fill-white ml-0.5" />
                </div>
              </div>
              {vid.duration && (
                <span className="absolute bottom-1.5 right-1.5 px-1.5 py-0.5 bg-slate-950/80 text-white rounded-md text-[10px] font-bold">
                  {vid.duration}
                </span>
              )}
            </div>

            {/* Info */}
            <div className="flex-1 flex flex-col justify-between py-1">
              <div>
                <span className="text-[11px] text-red-600 dark:text-red-400 font-bold uppercase tracking-wider">
                  {vid.source || 'YouTube'}
                </span>
                <h3 className="text-base font-semibold text-blue-700 dark:text-blue-400 group-hover:underline leading-snug mt-0.5 line-clamp-2">
                  {vid.title}
                </h3>
                <p className="text-xs text-slate-600 dark:text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                  {vid.snippet}
                </p>
              </div>

              <div className="mt-2 text-[11px] font-medium text-slate-400 dark:text-slate-400 flex items-center gap-2">
                <span className="text-slate-700 dark:text-slate-300 font-semibold">{vid.channel}</span>
                {vid.views && <span>• {vid.views}</span>}
              </div>
            </div>
          </a>
        );
      })}
    </div>
  );
};
