import React, { useState } from 'react';
import Markdown from 'react-markdown';
import { Sparkles, Volume2, VolumeX, Copy, Check, ExternalLink, ChevronDown, ChevronUp } from 'lucide-react';
import { GroundingSource } from '../types';

interface AIOverviewProps {
  summary: string;
  keyTakeaways?: string[];
  sources: GroundingSource[];
  query: string;
}

export const AIOverview: React.FC<AIOverviewProps> = ({
  summary,
  keyTakeaways,
  sources,
  query,
}) => {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [copied, setCopied] = useState(false);
  const [isExpanded, setIsExpanded] = useState(true);

  const handleSpeak = () => {
    if (!('speechSynthesis' in window)) return;

    if (isSpeaking) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
      return;
    }

    // Strip markdown formatting for speech
    const plainText = summary.replace(/[#*`_~\[\]()]/g, '');
    const utterance = new SpeechSynthesisUtterance(plainText);
    utterance.rate = 1.0;
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);

    window.speechSynthesis.speak(utterance);
    setIsSpeaking(true);
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(summary);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div id="ai-overview-box" className="mb-6 rounded-2xl bg-gradient-to-b from-blue-50/60 via-white to-slate-50/40 dark:from-slate-900 dark:via-slate-900/90 dark:to-slate-950 border border-blue-200/70 dark:border-blue-900/50 p-5 sm:p-6 shadow-xs transition-all">
      {/* Header */}
      <div className="flex items-center justify-between pb-3.5 border-b border-slate-200/70 dark:border-slate-800">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-gradient-to-br from-blue-600 via-indigo-600 to-violet-600 text-white shadow-2xs">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-bold text-slate-900 dark:text-white flex flex-wrap items-center gap-2">
              <span>AI Overview</span>
              <span className="text-[10px] font-bold text-blue-700 dark:text-blue-300 bg-blue-100/80 dark:bg-blue-950/80 px-2.5 py-0.5 rounded-full border border-blue-200 dark:border-blue-800">
                Gemini Grounded
              </span>
            </h3>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={handleSpeak}
            className={`p-2 rounded-lg transition-colors cursor-pointer text-xs font-medium flex items-center gap-1 ${
              isSpeaking
                ? 'bg-blue-600 text-white shadow-2xs'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
            title={isSpeaking ? 'Stop reading' : 'Listen to AI Overview'}
          >
            {isSpeaking ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
          </button>

          <button
            onClick={handleCopy}
            className="p-2 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
            title="Copy Overview text"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
          </button>

          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-2 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
            title={isExpanded ? 'Collapse Overview' : 'Expand Overview'}
          >
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="pt-4 space-y-4">
          {/* Main Markdown Text */}
          <div className="text-slate-800 dark:text-slate-200 text-sm sm:text-[15px] leading-relaxed prose dark:prose-invert max-w-none">
            <div className="markdown-body space-y-2.5 font-normal">
              <Markdown>{summary}</Markdown>
            </div>
          </div>

          {/* Key Takeaways */}
          {keyTakeaways && keyTakeaways.length > 0 && (
            <div className="bg-white dark:bg-slate-950/70 rounded-xl p-4 border border-slate-200/80 dark:border-slate-800 shadow-2xs">
              <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-blue-600" /> Key Insights:
              </h4>
              <ul className="space-y-2 text-xs sm:text-sm text-slate-700 dark:text-slate-300">
                {keyTakeaways.map((item, idx) => (
                  <li key={idx} className="flex items-start gap-2.5">
                    <span className="text-blue-600 dark:text-blue-400 font-bold mt-0.5">•</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Grounding Source Link Chips */}
          {sources && sources.length > 0 && (
            <div className="pt-3 border-t border-slate-200/70 dark:border-slate-800/80">
              <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider block mb-2">
                Sources & Citations:
              </span>
              <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
                {sources.map((src, idx) => {
                  const domain = src.domain || 'source';
                  const faviconUrl = `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
                  return (
                    <a
                      key={idx}
                      href={src.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex-shrink-0 flex items-center gap-2 bg-white dark:bg-slate-900 hover:bg-blue-50 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-700/80 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-800 dark:text-slate-200 transition-all shadow-2xs hover:border-blue-300 dark:hover:border-blue-700 group"
                    >
                      <img
                        src={faviconUrl}
                        alt=""
                        className="w-3.5 h-3.5 rounded-sm object-contain"
                        onError={(e) => {
                          (e.target as HTMLElement).style.display = 'none';
                        }}
                      />
                      <span className="max-w-[140px] truncate">{src.title || domain}</span>
                      <ExternalLink className="w-3 h-3 text-slate-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors" />
                    </a>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
