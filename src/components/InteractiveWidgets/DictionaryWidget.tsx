import React from 'react';
import { Volume2, BookOpen } from 'lucide-react';

interface DictionaryWidgetProps {
  data?: {
    word?: string;
    phonetic?: string;
    partOfSpeech?: string;
    definition?: string;
    example?: string;
    synonyms?: string[];
  };
}

export const DictionaryWidget: React.FC<DictionaryWidgetProps> = ({ data }) => {
  const word = data?.word || 'search';
  const phonetic = data?.phonetic || '/sɜːtʃ/';
  const partOfSpeech = data?.partOfSpeech || 'verb';
  const definition = data?.definition || 'To look through or explore carefully in order to find something, someone, or information.';
  const example = data?.example || 'He searched the web for relevant research articles.';
  const synonyms = data?.synonyms || ['explore', 'seek', 'investigate', 'inquire', 'hunt'];

  const playPronunciation = () => {
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(word);
      utterance.rate = 0.9;
      window.speechSynthesis.speak(utterance);
    }
  };

  return (
    <div id="dictionary-widget" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-2xs max-w-xl w-full my-4">
      <div className="flex items-center gap-2 pb-2 border-b border-slate-100 dark:border-slate-800 mb-3 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
        <BookOpen className="w-3.5 h-3.5 text-blue-500" /> Dictionary
      </div>

      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-2xl font-bold text-slate-900 dark:text-white capitalize flex items-center gap-3">
            {word}
          </h3>
          <p className="text-sm font-mono text-slate-500 dark:text-slate-400 mt-0.5">
            {phonetic}
          </p>
        </div>

        <button
          onClick={playPronunciation}
          className="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950/50 hover:bg-blue-100 dark:hover:bg-blue-900/50 text-blue-600 dark:text-blue-400 transition-colors cursor-pointer"
          title="Listen to pronunciation"
        >
          <Volume2 className="w-5 h-5" />
        </button>
      </div>

      <div className="mt-4 space-y-3">
        <div>
          <span className="text-xs font-semibold italic text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/40 px-2 py-0.5 rounded-lg">
            {partOfSpeech}
          </span>
          <p className="text-base text-slate-800 dark:text-slate-200 mt-2 leading-relaxed">
            {definition}
          </p>
          {example && (
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1.5 italic pl-3 border-l-2 border-slate-300 dark:border-slate-700">
              "{example}"
            </p>
          )}
        </div>

        {synonyms && synonyms.length > 0 && (
          <div className="pt-2">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400 block mb-1.5">
              Similar words:
            </span>
            <div className="flex flex-wrap gap-1.5">
              {synonyms.map((s, idx) => (
                <span
                  key={idx}
                  className="text-xs bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 px-2.5 py-1 rounded-lg cursor-pointer transition-colors"
                >
                  {s}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
