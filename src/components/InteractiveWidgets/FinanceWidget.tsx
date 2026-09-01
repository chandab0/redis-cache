import React from 'react';
import { TrendingUp, TrendingDown, DollarSign } from 'lucide-react';

interface FinanceWidgetProps {
  data?: {
    symbol?: string;
    price?: string | number;
    change?: string | number;
    changePercent?: string;
    currency?: string;
    marketCap?: string;
    high52?: string | number;
    low52?: string | number;
  };
}

export const FinanceWidget: React.FC<FinanceWidgetProps> = ({ data }) => {
  const symbol = data?.symbol || 'GOOGL (Alphabet Inc.)';
  const price = data?.price || '182.40';
  const change = data?.change || '+2.85';
  const changePercent = data?.changePercent || '+1.59%';
  const isPositive = !String(change).startsWith('-');
  const currency = data?.currency || 'USD';
  const marketCap = data?.marketCap || '$2.28T';
  const high52 = data?.high52 || '193.30';
  const low52 = data?.low52 || '130.50';

  return (
    <div id="finance-widget" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-2xs max-w-xl w-full my-4">
      <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800 mb-3">
        <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
          <DollarSign className="w-3.5 h-3.5 text-emerald-500" /> Market Summary
        </span>
        <span className="text-xs text-slate-400">Nasdaq • Real-time Data</span>
      </div>

      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-base font-bold text-slate-900 dark:text-white">
            {symbol}
          </h3>
          <div className="flex items-baseline gap-3 mt-1">
            <span className="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              {price} <span className="text-sm font-normal text-slate-500">{currency}</span>
            </span>
            <span
              className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-lg ${
                isPositive
                  ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/50'
                  : 'bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800/50'
              }`}
            >
              {isPositive ? <TrendingUp className="w-3.5 h-3.5" /> : <TrendingDown className="w-3.5 h-3.5" />}
              {change} ({changePercent})
            </span>
          </div>
        </div>
      </div>

      {/* Mini Visual Trend Chart (SVG) */}
      <div className="my-4 h-16 w-full">
        <svg className="w-full h-full" viewBox="0 0 300 60" preserveAspectRatio="none">
          <defs>
            <linearGradient id="trendGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor={isPositive ? '#10b981' : '#f43f5e'} stopOpacity="0.25" />
              <stop offset="100%" stopColor={isPositive ? '#10b981' : '#f43f5e'} stopOpacity="0" />
            </linearGradient>
          </defs>
          <path
            d="M 0,45 Q 40,30 80,38 T 150,20 T 220,25 T 300,10 L 300,60 L 0,60 Z"
            fill="url(#trendGradient)"
          />
          <path
            d="M 0,45 Q 40,30 80,38 T 150,20 T 220,25 T 300,10"
            fill="none"
            stroke={isPositive ? '#10b981' : '#f43f5e'}
            strokeWidth="2.5"
          />
        </svg>
      </div>

      <div className="grid grid-cols-3 gap-3 pt-2 border-t border-slate-100 dark:border-slate-800 text-xs text-slate-500 dark:text-slate-400">
        <div>
          <span>Mkt cap</span>
          <p className="font-semibold text-slate-900 dark:text-white mt-0.5">{marketCap}</p>
        </div>
        <div>
          <span>52-wk high</span>
          <p className="font-semibold text-slate-900 dark:text-white mt-0.5">{high52}</p>
        </div>
        <div>
          <span>52-wk low</span>
          <p className="font-semibold text-slate-900 dark:text-white mt-0.5">{low52}</p>
        </div>
      </div>
    </div>
  );
};
