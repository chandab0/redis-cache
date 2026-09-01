import React from 'react';
import {
  Search,
  Sparkles,
  MapPin,
  Newspaper,
  Youtube,
  Languages,
  HardDrive,
  Image as ImageIcon,
  Mail,
  Calendar,
  CloudSun,
  Calculator,
} from 'lucide-react';

interface AppsLauncherProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectApp: (query: string) => void;
}

export const AppsLauncher: React.FC<AppsLauncherProps> = ({
  isOpen,
  onClose,
  onSelectApp,
}) => {
  if (!isOpen) return null;

  const apps = [
    { name: 'Search', icon: <Search className="w-6 h-6 text-[#4285F4]" />, query: '' },
    { name: 'Gemini AI', icon: <Sparkles className="w-6 h-6 text-indigo-500" />, query: 'Gemini artificial intelligence overview' },
    { name: 'Maps', icon: <MapPin className="w-6 h-6 text-[#34A853]" />, query: 'Google Maps locations and navigation' },
    { name: 'News', icon: <Newspaper className="w-6 h-6 text-[#EA4335]" />, query: 'World news headlines today' },
    { name: 'YouTube', icon: <Youtube className="w-6 h-6 text-red-600" />, query: 'YouTube trending videos' },
    { name: 'Translate', icon: <Languages className="w-6 h-6 text-[#4285F4]" />, query: 'Google Translate language translation' },
    { name: 'Drive', icon: <HardDrive className="w-6 h-6 text-[#FBBC05]" />, query: 'Google Drive cloud storage' },
    { name: 'Photos', icon: <ImageIcon className="w-6 h-6 text-[#EA4335]" />, query: 'Google Photos gallery and visual albums' },
    { name: 'Gmail', icon: <Mail className="w-6 h-6 text-red-500" />, query: 'Gmail webmail service' },
    { name: 'Calendar', icon: <Calendar className="w-6 h-6 text-[#4285F4]" />, query: 'Calendar scheduling and events' },
    { name: 'Weather', icon: <CloudSun className="w-6 h-6 text-amber-500" />, query: 'weather today forecast' },
    { name: 'Calculator', icon: <Calculator className="w-6 h-6 text-emerald-500" />, query: 'calculator' },
  ];

  return (
    <>
      <div className="fixed inset-0 z-40" onClick={onClose} />
      <div className="absolute right-0 top-12 z-50 w-80 bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 p-4 animate-in fade-in zoom-in-95 duration-150">
        <div className="grid grid-cols-3 gap-2">
          {apps.map((app, idx) => (
            <button
              key={idx}
              onClick={() => {
                onClose();
                if (app.query) onSelectApp(app.query);
              }}
              className="flex flex-col items-center justify-center p-3 rounded-2xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors group cursor-pointer"
            >
              <div className="w-10 h-10 rounded-2xl bg-slate-50 dark:bg-slate-950 flex items-center justify-center group-hover:scale-105 transition-transform shadow-2xs border border-slate-100 dark:border-slate-800">
                {app.icon}
              </div>
              <span className="text-xs font-medium text-slate-700 dark:text-slate-300 mt-2 truncate w-full text-center">
                {app.name}
              </span>
            </button>
          ))}
        </div>
      </div>
    </>
  );
};
