import React, { useState } from 'react';
import { ArrowRightLeft, RefreshCw } from 'lucide-react';

interface UnitConverterProps {
  data?: {
    fromValue?: number | string;
    fromUnit?: string;
    toValue?: number | string;
    toUnit?: string;
    rate?: number | string;
  };
}

const CONVERSIONS: Record<string, { [key: string]: number }> = {
  currency: {
    USD: 1,
    EUR: 0.92,
    GBP: 0.79,
    JPY: 154.5,
    CAD: 1.36,
    AUD: 1.52,
    INR: 83.4,
    CNY: 7.24,
  },
  length: {
    meters: 1,
    kilometers: 0.001,
    centimeters: 100,
    miles: 0.000621371,
    feet: 3.28084,
    inches: 39.3701,
  },
  mass: {
    kilograms: 1,
    grams: 1000,
    pounds: 2.20462,
    ounces: 35.274,
  },
};

export const UnitConverterWidget: React.FC<UnitConverterProps> = ({ data }) => {
  const [category, setCategory] = useState<'currency' | 'length' | 'mass'>('currency');
  const [fromUnit, setFromUnit] = useState<string>(data?.fromUnit || (category === 'currency' ? 'USD' : category === 'length' ? 'meters' : 'kilograms'));
  const [toUnit, setToUnit] = useState<string>(data?.toUnit || (category === 'currency' ? 'EUR' : category === 'length' ? 'feet' : 'pounds'));
  const [fromValue, setFromValue] = useState<string>(String(data?.fromValue || '1'));

  const calculateToValue = () => {
    const num = parseFloat(fromValue);
    if (isNaN(num)) return '0';
    const catMap = CONVERSIONS[category];
    if (!catMap || !catMap[fromUnit] || !catMap[toUnit]) {
      return String(data?.toValue || num);
    }
    // Convert from -> base -> to
    const inBase = num / catMap[fromUnit];
    const converted = inBase * catMap[toUnit];
    return String(Number(converted.toFixed(4)));
  };

  const handleSwap = () => {
    const temp = fromUnit;
    setFromUnit(toUnit);
    setToUnit(temp);
  };

  const unitsList = Object.keys(CONVERSIONS[category] || {});

  return (
    <div id="unit-converter-widget" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-2xs max-w-lg w-full my-4">
      <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800 mb-4">
        <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
          Unit & Currency Converter
        </span>
        <div className="flex gap-1 text-xs">
          {(['currency', 'length', 'mass'] as const).map((cat) => (
            <button
              key={cat}
              onClick={() => {
                setCategory(cat);
                const keys = Object.keys(CONVERSIONS[cat]);
                setFromUnit(keys[0]);
                setToUnit(keys[1] || keys[0]);
              }}
              className={`px-2.5 py-1 rounded-lg capitalize font-medium transition-colors cursor-pointer ${
                category === cat
                  ? 'bg-blue-600 text-white shadow-2xs'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-5 gap-3 items-center">
        {/* From Side */}
        <div className="sm:col-span-2 space-y-2">
          <input
            type="number"
            value={fromValue}
            onChange={(e) => setFromValue(e.target.value)}
            className="w-full text-xl font-semibold px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-blue-500"
          />
          <select
            value={fromUnit}
            onChange={(e) => setFromUnit(e.target.value)}
            className="w-full text-sm font-medium px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-800 dark:text-slate-200 cursor-pointer"
          >
            {unitsList.map((u) => (
              <option key={u} value={u}>
                {u}
              </option>
            ))}
          </select>
        </div>

        {/* Swap Button */}
        <div className="sm:col-span-1 flex justify-center py-1">
          <button
            onClick={handleSwap}
            className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 transition-colors cursor-pointer shadow-2xs"
            title="Swap Units"
          >
            <ArrowRightLeft className="w-4 h-4" />
          </button>
        </div>

        {/* To Side */}
        <div className="sm:col-span-2 space-y-2">
          <div className="w-full text-xl font-semibold px-3 py-2 bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-900/50 rounded-xl text-blue-900 dark:text-blue-200 overflow-x-auto">
            {calculateToValue()}
          </div>
          <select
            value={toUnit}
            onChange={(e) => setToUnit(e.target.value)}
            className="w-full text-sm font-medium px-3 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl text-slate-800 dark:text-slate-200 cursor-pointer"
          >
            {unitsList.map((u) => (
              <option key={u} value={u}>
                {u}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mt-3 text-xs text-slate-500 dark:text-slate-400 flex items-center justify-between">
        <span>1 {fromUnit} = {(CONVERSIONS[category]?.[toUnit] / CONVERSIONS[category]?.[fromUnit]).toFixed(4)} {toUnit}</span>
        <span className="flex items-center gap-1">
          <RefreshCw className="w-3 h-3 text-slate-400" /> Live market rates
        </span>
      </div>
    </div>
  );
};
