import React from 'react';
import { Globe, ExternalLink, Share2, Info } from 'lucide-react';
import { KnowledgePanelData } from '../types';

interface KnowledgePanelProps {
  data: KnowledgePanelData;
  onRelatedClick?: (query: string) => void;
}

export const KnowledgePanel: React.FC<KnowledgePanelProps> = ({
  data,
  onRelatedClick,
}) => {
  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: data.title,
        text: data.description,
        url: window.location.href,
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(window.location.href);
    }
  };

  return (
    <div id="knowledge-panel" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-xs w-full lg:max-w-sm mb-6 lg:mb-0">
      {/* Entity Header Image if available */}
      {data.imageUrl && (
        <div className="relative h-48 w-full bg-slate-100 dark:bg-slate-800 overflow-hidden">
          <img
            src={data.imageUrl}
            alt={data.title}
            className="w-full h-full object-cover"
            onError={(e) => {
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-transparent to-transparent" />
          <div className="absolute bottom-3 left-4 right-4 text-white">
            <h2 className="text-xl font-bold tracking-tight text-shadow-sm">{data.title}</h2>
            {data.subtitle && (
              <p className="text-xs text-slate-200 font-medium">{data.subtitle}</p>
            )}
          </div>
        </div>
      )}

      <div className="p-5">
        {!data.imageUrl && (
          <div className="border-b border-slate-200/80 dark:border-slate-800 pb-3 mb-3">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-white tracking-tight">
                  {data.title}
                </h2>
                {data.subtitle && (
                  <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mt-0.5">
                    {data.subtitle}
                  </p>
                )}
              </div>
              <button
                onClick={handleShare}
                className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                title="Share Knowledge Card"
              >
                <Share2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* Quick Description */}
        <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed mb-4">
          {data.description}
          {data.wikipediaUrl && (
            <a
              href={data.wikipediaUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 dark:text-blue-400 hover:underline ml-1 inline-flex items-center text-xs font-semibold"
            >
              Wikipedia <ExternalLink className="w-3 h-3 ml-0.5" />
            </a>
          )}
        </p>

        {/* Action Buttons */}
        <div className="flex gap-2 mb-4">
          {data.websiteUrl && (
            <a
              href={data.websiteUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 py-2 px-3 bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/40 text-blue-600 dark:text-blue-300 border border-blue-200/80 dark:border-blue-900/50 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors"
            >
              <Globe className="w-3.5 h-3.5" /> Official Website
            </a>
          )}
        </div>

        {/* Key Attributes Grid */}
        {data.attributes && data.attributes.length > 0 && (
          <div className="border-t border-slate-200/80 dark:border-slate-800 pt-3 space-y-2">
            <h4 className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
              <Info className="w-3 h-3 text-slate-400" /> Key Facts
            </h4>
            {data.attributes.map((attr, idx) => (
              <div key={idx} className="text-xs grid grid-cols-3 gap-2">
                <span className="font-semibold text-slate-500 dark:text-slate-400">
                  {attr.label}:
                </span>
                <span className="col-span-2 text-slate-800 dark:text-slate-200 font-medium">
                  {attr.value}
                </span>
              </div>
            ))}
          </div>
        )}

        {/* Social / Related Links */}
        {data.socialLinks && data.socialLinks.length > 0 && (
          <div className="border-t border-slate-200/80 dark:border-slate-800 pt-3 mt-3">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-2">
              Profiles:
            </span>
            <div className="flex flex-wrap gap-2">
              {data.socialLinks.map((soc, idx) => (
                <a
                  key={idx}
                  href={soc.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs font-medium text-slate-700 dark:text-slate-300 transition-colors border border-slate-200/60 dark:border-slate-700/60"
                >
                  {soc.platform}
                </a>
              ))}
            </div>
          </div>
        )}

        {/* Related Entities */}
        {data.relatedEntities && data.relatedEntities.length > 0 && (
          <div className="border-t border-slate-200/80 dark:border-slate-800 pt-3 mt-3">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-2">
              People also search for:
            </span>
            <div className="flex flex-wrap gap-1.5">
              {data.relatedEntities.map((ent, idx) => (
                <button
                  key={idx}
                  onClick={() => onRelatedClick && onRelatedClick(ent.name)}
                  className="text-xs px-2.5 py-1 rounded-full bg-slate-50 dark:bg-slate-800 hover:bg-blue-50 dark:hover:bg-blue-950/40 border border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200 transition-colors cursor-pointer"
                >
                  {ent.name}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
