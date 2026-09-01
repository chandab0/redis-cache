import React, { useState } from 'react';
import { Cloud, CloudRain, Sun, Wind, Droplets, Compass } from 'lucide-react';

interface WeatherWidgetProps {
  data?: {
    location?: string;
    temperature?: string | number;
    condition?: string;
    humidity?: string | number;
    wind?: string;
    high?: string | number;
    low?: string | number;
    forecast?: { day: string; temp: string | number; condition: string }[];
  };
}

export const WeatherWidget: React.FC<WeatherWidgetProps> = ({ data }) => {
  const [unit, setUnit] = useState<'C' | 'F'>('C');

  const location = data?.location || 'San Francisco, CA';
  const rawTemp = typeof data?.temperature === 'number' ? data.temperature : parseInt(data?.temperature || '21', 10) || 21;
  const condition = data?.condition || 'Partly Cloudy';
  const humidity = data?.humidity || '68%';
  const wind = data?.wind || '14 km/h';
  const high = data?.high || rawTemp + 3;
  const low = data?.low || rawTemp - 4;

  const toDisplayTemp = (celsius: number) => {
    if (unit === 'F') {
      return Math.round((celsius * 9) / 5 + 32);
    }
    return Math.round(celsius);
  };

  const defaultForecast = [
    { day: 'Mon', temp: rawTemp, condition: 'Sunny' },
    { day: 'Tue', temp: rawTemp + 1, condition: 'Partly Cloudy' },
    { day: 'Wed', temp: rawTemp - 2, condition: 'Rain' },
    { day: 'Thu', temp: rawTemp - 1, condition: 'Cloudy' },
    { day: 'Fri', temp: rawTemp + 2, condition: 'Sunny' },
    { day: 'Sat', temp: rawTemp + 3, condition: 'Sunny' },
    { day: 'Sun', temp: rawTemp + 1, condition: 'Partly Cloudy' },
  ];

  const forecast = data?.forecast && data.forecast.length > 0 ? data.forecast : defaultForecast;

  const renderWeatherIcon = (cond: string, size = 'w-8 h-8') => {
    const c = cond.toLowerCase();
    if (c.includes('rain') || c.includes('shower') || c.includes('drizzle')) {
      return <CloudRain className={`${size} text-blue-500`} />;
    }
    if (c.includes('cloud') || c.includes('overcast')) {
      return <Cloud className={`${size} text-gray-500`} />;
    }
    return <Sun className={`${size} text-amber-500`} />;
  };

  return (
    <div id="weather-widget" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-2xs max-w-xl w-full my-4">
      <div className="flex items-center justify-between mb-3">
        <div>
          <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <span>{location}</span>
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400">Weather Forecast</p>
        </div>

        {/* Unit switch */}
        <div className="flex items-center bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-0.5 text-xs font-semibold">
          <button
            onClick={() => setUnit('C')}
            className={`px-2.5 py-1 rounded-lg transition-colors cursor-pointer ${
              unit === 'C'
                ? 'bg-blue-600 text-white shadow-2xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            °C
          </button>
          <button
            onClick={() => setUnit('F')}
            className={`px-2.5 py-1 rounded-lg transition-colors cursor-pointer ${
              unit === 'F'
                ? 'bg-blue-600 text-white shadow-2xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            °F
          </button>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 py-2 border-b border-slate-100 dark:border-slate-800">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-2xl shadow-2xs border border-slate-100 dark:border-slate-800">
            {renderWeatherIcon(condition, 'w-10 h-10')}
          </div>
          <div>
            <div className="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              {toDisplayTemp(rawTemp)}°{unit}
            </div>
            <div className="text-xs font-medium text-slate-700 dark:text-slate-300">
              {condition}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-x-6 gap-y-1.5 text-xs text-slate-600 dark:text-slate-400">
          <div className="flex items-center gap-1.5">
            <Droplets className="w-3.5 h-3.5 text-blue-500" />
            <span>Humidity: <strong className="text-slate-900 dark:text-white font-semibold">{humidity}</strong></span>
          </div>
          <div className="flex items-center gap-1.5">
            <Wind className="w-3.5 h-3.5 text-teal-500" />
            <span>Wind: <strong className="text-slate-900 dark:text-white font-semibold">{wind}</strong></span>
          </div>
          <div className="flex items-center gap-1.5">
            <Compass className="w-3.5 h-3.5 text-indigo-500" />
            <span>High: <strong className="text-slate-900 dark:text-white font-semibold">{toDisplayTemp(typeof high === 'number' ? high : rawTemp + 3)}°</strong></span>
          </div>
          <div className="flex items-center gap-1.5">
            <Compass className="w-3.5 h-3.5 text-indigo-500" />
            <span>Low: <strong className="text-slate-900 dark:text-white font-semibold">{toDisplayTemp(typeof low === 'number' ? low : rawTemp - 4)}°</strong></span>
          </div>
        </div>
      </div>

      {/* 7-Day mini forecast */}
      <div className="grid grid-cols-7 gap-1 pt-3 text-center">
        {forecast.slice(0, 7).map((item, idx) => {
          const itemTemp = typeof item.temp === 'number' ? item.temp : parseInt(String(item.temp), 10) || rawTemp;
          return (
            <div
              key={idx}
              className="p-1.5 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
            >
              <div className="text-[11px] font-medium text-slate-500 dark:text-slate-400">{item.day}</div>
              <div className="my-1 flex justify-center">
                {renderWeatherIcon(item.condition, 'w-5 h-5')}
              </div>
              <div className="text-xs font-semibold text-slate-900 dark:text-white">
                {toDisplayTemp(itemTemp)}°
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
