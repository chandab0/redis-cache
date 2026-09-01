import React, { useState } from 'react';
import { Delete, Equal, RotateCcw } from 'lucide-react';

interface CalculatorWidgetProps {
  initialExpression?: string;
  initialResult?: string;
}

export const CalculatorWidget: React.FC<CalculatorWidgetProps> = ({
  initialExpression = '',
  initialResult = '0',
}) => {
  const [display, setDisplay] = useState<string>(initialResult !== '0' ? initialResult : '0');
  const [equation, setEquation] = useState<string>(initialExpression);
  const [isNewNumber, setIsNewNumber] = useState<boolean>(true);

  const handleDigit = (digit: string) => {
    if (isNewNumber || display === '0') {
      setDisplay(digit);
      setIsNewNumber(false);
    } else {
      setDisplay(display + digit);
    }
  };

  const handleOperator = (op: string) => {
    setEquation(`${display} ${op}`);
    setIsNewNumber(true);
  };

  const handleClear = () => {
    setDisplay('0');
    setEquation('');
    setIsNewNumber(true);
  };

  const handleBackspace = () => {
    if (display.length > 1) {
      setDisplay(display.slice(0, -1));
    } else {
      setDisplay('0');
      setIsNewNumber(true);
    }
  };

  const handleEqual = () => {
    if (!equation) return;
    try {
      const fullExp = `${equation} ${display}`
        .replace(/×/g, '*')
        .replace(/÷/g, '/')
        .replace(/−/g, '-');
      // safe simple evaluation
      // eslint-disable-next-line no-new-func
      const result = Function(`'use strict'; return (${fullExp})`)();
      if (typeof result === 'number' && !isNaN(result) && isFinite(result)) {
        setEquation(`${equation} ${display} =`);
        setDisplay(String(Number(result.toFixed(8))));
        setIsNewNumber(true);
      }
    } catch {
      setDisplay('Error');
      setIsNewNumber(true);
    }
  };

  const handleScientific = (func: string) => {
    const val = parseFloat(display);
    if (isNaN(val)) return;
    let res = 0;
    switch (func) {
      case 'sin':
        res = Math.sin((val * Math.PI) / 180);
        break;
      case 'cos':
        res = Math.cos((val * Math.PI) / 180);
        break;
      case 'tan':
        res = Math.tan((val * Math.PI) / 180);
        break;
      case 'sqrt':
        res = Math.sqrt(val);
        break;
      case 'sq':
        res = Math.pow(val, 2);
        break;
      case 'log':
        res = Math.log10(val);
        break;
      case 'ln':
        res = Math.log(val);
        break;
      case 'inv':
        res = 1 / val;
        break;
      default:
        return;
    }
    setEquation(`${func}(${display}) =`);
    setDisplay(String(Number(res.toFixed(8))));
    setIsNewNumber(true);
  };

  return (
    <div id="calculator-widget" className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 shadow-2xs max-w-md w-full my-4">
      <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800 mb-3">
        <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
          Calculator
        </span>
        <button
          onClick={handleClear}
          className="text-xs text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1 cursor-pointer font-medium"
        >
          <RotateCcw className="w-3 h-3" /> Reset
        </button>
      </div>

      <div className="bg-slate-50 dark:bg-slate-950 rounded-xl p-3.5 mb-3 text-right border border-slate-200/60 dark:border-slate-800">
        <div className="text-xs text-slate-500 dark:text-slate-400 h-4 overflow-hidden text-ellipsis font-mono">
          {equation}
        </div>
        <div className="text-2xl font-mono font-bold text-slate-900 dark:text-white tracking-tight overflow-x-auto">
          {display}
        </div>
      </div>

      <div className="grid grid-cols-5 gap-1.5 text-sm font-medium">
        {/* Row 1 */}
        <button
          onClick={() => handleScientific('sin')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          sin
        </button>
        <button
          onClick={() => handleScientific('cos')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          cos
        </button>
        <button
          onClick={() => handleScientific('tan')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          tan
        </button>
        <button
          onClick={handleClear}
          className="py-2.5 bg-red-100 dark:bg-red-950/50 text-red-700 dark:text-red-300 rounded-xl hover:bg-red-200 dark:hover:bg-red-900/50 transition-colors font-semibold cursor-pointer text-xs"
        >
          AC
        </button>
        <button
          onClick={handleBackspace}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors flex items-center justify-center cursor-pointer"
        >
          <Delete className="w-4 h-4" />
        </button>

        {/* Row 2 */}
        <button
          onClick={() => handleScientific('sqrt')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          √
        </button>
        <button
          onClick={() => handleDigit('7')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          7
        </button>
        <button
          onClick={() => handleDigit('8')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          8
        </button>
        <button
          onClick={() => handleDigit('9')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          9
        </button>
        <button
          onClick={() => handleOperator('÷')}
          className="py-2.5 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 rounded-xl hover:bg-blue-100 dark:hover:bg-blue-900/40 font-bold transition-colors cursor-pointer"
        >
          ÷
        </button>

        {/* Row 3 */}
        <button
          onClick={() => handleScientific('sq')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          x²
        </button>
        <button
          onClick={() => handleDigit('4')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          4
        </button>
        <button
          onClick={() => handleDigit('5')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          5
        </button>
        <button
          onClick={() => handleDigit('6')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          6
        </button>
        <button
          onClick={() => handleOperator('×')}
          className="py-2.5 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 rounded-xl hover:bg-blue-100 dark:hover:bg-blue-900/40 font-bold transition-colors cursor-pointer"
        >
          ×
        </button>

        {/* Row 4 */}
        <button
          onClick={() => handleScientific('log')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          log
        </button>
        <button
          onClick={() => handleDigit('1')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          1
        </button>
        <button
          onClick={() => handleDigit('2')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          2
        </button>
        <button
          onClick={() => handleDigit('3')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          3
        </button>
        <button
          onClick={() => handleOperator('−')}
          className="py-2.5 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 rounded-xl hover:bg-blue-100 dark:hover:bg-blue-900/40 font-bold transition-colors cursor-pointer"
        >
          −
        </button>

        {/* Row 5 */}
        <button
          onClick={() => handleScientific('inv')}
          className="py-2.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-xl hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors cursor-pointer text-xs font-semibold"
        >
          1/x
        </button>
        <button
          onClick={() => handleDigit('0')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-semibold cursor-pointer"
        >
          0
        </button>
        <button
          onClick={() => handleDigit('.')}
          className="py-2.5 bg-white dark:bg-slate-900 text-slate-900 dark:text-white border border-slate-200 dark:border-slate-700 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-bold cursor-pointer"
        >
          .
        </button>
        <button
          onClick={handleEqual}
          className="py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold transition-colors flex items-center justify-center cursor-pointer shadow-2xs"
        >
          <Equal className="w-5 h-5" />
        </button>
        <button
          onClick={() => handleOperator('+')}
          className="py-2.5 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 rounded-xl hover:bg-blue-100 dark:hover:bg-blue-900/40 font-bold transition-colors cursor-pointer"
        >
          +
        </button>
      </div>
    </div>
  );
};
