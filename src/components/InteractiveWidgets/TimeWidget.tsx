import React, { useState, useEffect } from 'react';
import { Clock, Globe } from 'lucide-react';

interface TimeWidgetProps {
  data?: {
    location?: string;
    time?: string;
    timezone?: string;
    date?: string;
  };
}

export const TimeWidget: React.FC<TimeWidgetProps> = ({ data }) => {
  const [currentTime, setCurrentTime] = useState<string>(
    data?.time || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  );

  useEffect(() => {
    const timer = setInterval(() => {
      if (!data?.time) {
        setCurrentTime(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [data?.time]);

  const location = data?.location || 'Local Time';
  const timezone = data?.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone;
  const dateStr = data?.date || new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  return (
    <div id="time-widget" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-2xs max-w-md w-full my-4">
      <div className="flex items-center gap-1.5 pb-2 border-b border-slate-100 dark:border-slate-800 mb-3 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
        <Clock className="w-3.5 h-3.5 text-blue-500" /> Time & Clock
      </div>

      <div className="flex items-baseline justify-between">
        <div>
          <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight font-mono">
            {currentTime}
          </div>
          <div className="text-xs font-medium text-slate-700 dark:text-slate-300 mt-1">
            {dateStr}
          </div>
        </div>
      </div>

      <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
        <span className="flex items-center gap-1">
          <Globe className="w-3 h-3 text-blue-500" /> {location}
        </span>
        <span className="font-mono">{timezone}</span>
      </div>
    </div>
  );
};
