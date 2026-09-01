import React, { useState } from 'react';
import { X, Sliders, Shield, Globe, Moon, Sun, Trash2, History, Check } from 'lucide-react';
import { SearchSettings, HistoryItem } from '../types';
import { getHistory, deleteHistoryItem, clearAllHistory, saveStoredSettings } from '../services/api';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: SearchSettings;
  onUpdateSettings: (settings: SearchSettings) => void;
  onSelectQuery: (query: string) => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
  onSelectQuery,
}) => {
  const [localSettings, setLocalSettings] = useState<SearchSettings>(settings);
  const [activeTab, setActiveTab] = useState<'preferences' | 'history'>('preferences');
  const [historyList, setHistoryList] = useState<HistoryItem[]>(getHistory());
  const [saveToast, setSaveToast] = useState<boolean>(false);

  if (!isOpen) return null;

  const handleSave = () => {
    onUpdateSettings(localSettings);
    saveStoredSettings(localSettings);
    setSaveToast(true);
    setTimeout(() => {
      setSaveToast(false);
      onClose();
    }, 600);
  };

  const handleDeleteHistory = (id: string) => {
    const updated = deleteHistoryItem(id);
    setHistoryList(updated);
  };

  const handleClearAllHistory = () => {
    if (window.confirm('Are you sure you want to clear your entire search history?')) {
      clearAllHistory();
      setHistoryList([]);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-2xl w-full overflow-hidden shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-4 px-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">
                Search Settings & History
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Customize your search experience and privacy preferences
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switch */}
        <div className="flex border-b border-slate-100 dark:border-slate-800 px-6 pt-2">
          <button
            onClick={() => setActiveTab('preferences')}
            className={`pb-3 px-3 text-xs font-bold uppercase tracking-wider border-b-2 transition-colors cursor-pointer ${
              activeTab === 'preferences'
                ? 'border-blue-600 text-blue-600 dark:text-blue-400'
                : 'border-transparent text-slate-400 hover:text-slate-900 dark:hover:text-slate-300'
            }`}
          >
            Preferences
          </button>
          <button
            onClick={() => {
              setActiveTab('history');
              setHistoryList(getHistory());
            }}
            className={`pb-3 px-3 text-xs font-bold uppercase tracking-wider border-b-2 transition-colors cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'history'
                ? 'border-blue-600 text-blue-600 dark:text-blue-400'
                : 'border-transparent text-slate-400 hover:text-slate-900 dark:hover:text-slate-300'
            }`}
          >
            <History className="w-3.5 h-3.5" /> Search History ({historyList.length})
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto flex-1">
          {activeTab === 'preferences' ? (
            <div className="space-y-6">
              {/* SafeSearch */}
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Shield className="w-4 h-4 text-emerald-500" />
                  <h4 className="text-sm font-bold text-slate-900 dark:text-white">SafeSearch Filter</h4>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Filter explicit content like pornography or violence from search results.
                </p>
                <div className="grid grid-cols-3 gap-2 pt-1">
                  {(['strict', 'moderate', 'off'] as const).map((mode) => (
                    <button
                      key={mode}
                      onClick={() => setLocalSettings({ ...localSettings, safeSearch: mode })}
                      className={`p-2.5 rounded-xl border text-xs font-semibold capitalize transition-all cursor-pointer ${
                        localSettings.safeSearch === mode
                          ? 'border-blue-600 bg-blue-50/60 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 shadow-2xs'
                          : 'border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {mode}
                    </button>
                  ))}
                </div>
              </div>

              {/* Theme Selection */}
              <div className="space-y-2 pt-4 border-t border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-2">
                  <Moon className="w-4 h-4 text-indigo-500" />
                  <h4 className="text-sm font-bold text-slate-900 dark:text-white">Appearance & Theme</h4>
                </div>
                <div className="grid grid-cols-3 gap-2 pt-1">
                  {(['light', 'dark', 'system'] as const).map((t) => (
                    <button
                      key={t}
                      onClick={() => setLocalSettings({ ...localSettings, theme: t })}
                      className={`p-2.5 rounded-xl border text-xs font-semibold capitalize transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                        localSettings.theme === t
                          ? 'border-blue-600 bg-blue-50/60 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 shadow-2xs'
                          : 'border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {t === 'light' ? <Sun className="w-3.5 h-3.5" /> : <Moon className="w-3.5 h-3.5" />}
                      {t}
                    </button>
                  ))}
                </div>
              </div>

              {/* Region */}
              <div className="space-y-2 pt-4 border-t border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-2">
                  <Globe className="w-4 h-4 text-blue-500" />
                  <h4 className="text-sm font-bold text-slate-900 dark:text-white">Region for Results</h4>
                </div>
                <select
                  value={localSettings.region}
                  onChange={(e) => setLocalSettings({ ...localSettings, region: e.target.value })}
                  className="w-full text-xs font-medium px-3 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-900 dark:text-white cursor-pointer"
                >
                  <option value="United States">United States (English)</option>
                  <option value="United Kingdom">United Kingdom (English)</option>
                  <option value="Canada">Canada (English / French)</option>
                  <option value="Germany">Germany (German)</option>
                  <option value="France">France (French)</option>
                  <option value="Japan">Japan (Japanese)</option>
                  <option value="India">India (English / Hindi)</option>
                  <option value="Australia">Australia (English)</option>
                  <option value="Global">Global / Current Location</option>
                </select>
              </div>

              {/* Open in new tab */}
              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-bold text-slate-900 dark:text-white">Open in new tab</h4>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Open each clicked search result in a new browser tab.</p>
                </div>
                <input
                  type="checkbox"
                  checked={localSettings.openInNewTab}
                  onChange={(e) => setLocalSettings({ ...localSettings, openInNewTab: e.target.checked })}
                  className="w-4 h-4 text-blue-600 rounded-md cursor-pointer accent-blue-600"
                />
              </div>
            </div>
          ) : (
            /* Search History Tab */
            <div className="space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  Your search history is stored privately in your browser storage.
                </span>
                {historyList.length > 0 && (
                  <button
                    onClick={handleClearAllHistory}
                    className="text-xs font-bold text-red-600 dark:text-red-400 hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <Trash2 className="w-3.5 h-3.5" /> Clear All History
                  </button>
                )}
              </div>

              {historyList.length === 0 ? (
                <div className="py-12 text-center text-slate-400">
                  <History className="w-8 h-8 mx-auto mb-2 opacity-50" />
                  <p className="text-sm">No search history recorded yet.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {historyList.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between p-3 rounded-xl bg-slate-50 dark:bg-slate-950 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors group"
                    >
                      <button
                        onClick={() => {
                          onClose();
                          onSelectQuery(item.query);
                        }}
                        className="text-left flex-1 text-xs sm:text-sm font-medium text-slate-800 dark:text-slate-200 hover:text-blue-600 truncate mr-3 cursor-pointer"
                      >
                        {item.query}
                        <span className="text-[10px] text-slate-400 block mt-0.5">
                          {new Date(item.timestamp).toLocaleDateString()} at {new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </button>

                      <button
                        onClick={() => handleDeleteHistory(item.id)}
                        className="p-1.5 text-slate-400 hover:text-red-500 rounded-lg hover:bg-white dark:hover:bg-slate-800 transition-colors cursor-pointer"
                        title="Delete from history"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="p-4 px-6 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between bg-slate-50/70 dark:bg-slate-950">
          <div>
            {saveToast && (
              <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                <Check className="w-4 h-4" /> Preferences saved!
              </span>
            )}
          </div>

          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-800 rounded-xl transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-5 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white rounded-xl shadow-xs transition-colors cursor-pointer"
            >
              Save Settings
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
