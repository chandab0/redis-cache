import React, { useEffect, useState } from 'react';
import { Sparkles, Dices, Coins, RefreshCw } from 'lucide-react';

interface EasterEggBannerProps {
  effect: 'barrel_roll' | 'askew' | 'coin_flip' | 'roll_dice';
  onDismiss?: () => void;
}

export const EasterEggBanner: React.FC<EasterEggBannerProps> = ({ effect, onDismiss }) => {
  const [coinResult, setCoinResult] = useState<'HEADS' | 'TAILS' | null>(null);
  const [isFlipping, setIsFlipping] = useState(false);
  const [diceResult, setDiceResult] = useState<number | null>(null);
  const [isRolling, setIsRolling] = useState(false);

  useEffect(() => {
    if (effect === 'barrel_roll') {
      const root = document.getElementById('root');
      if (root) {
        root.style.transition = 'transform 2.5s cubic-bezier(0.4, 0, 0.2, 1)';
        root.style.transform = 'rotate(360deg)';
        const timer = setTimeout(() => {
          root.style.transform = 'none';
        }, 2600);
        return () => {
          clearTimeout(timer);
          root.style.transform = 'none';
        };
      }
    } else if (effect === 'askew') {
      const main = document.querySelector('main');
      if (main) {
        main.style.transition = 'transform 0.5s ease';
        main.style.transform = 'rotate(1.2deg)';
        return () => {
          main.style.transform = 'none';
        };
      }
    } else if (effect === 'coin_flip') {
      flipCoin();
    } else if (effect === 'roll_dice') {
      rollDice();
    }
  }, [effect]);

  const flipCoin = () => {
    setIsFlipping(true);
    setTimeout(() => {
      setCoinResult(Math.random() > 0.5 ? 'HEADS' : 'TAILS');
      setIsFlipping(false);
    }, 800);
  };

  const rollDice = () => {
    setIsRolling(true);
    setTimeout(() => {
      setDiceResult(Math.floor(Math.random() * 6) + 1);
      setIsRolling(false);
    }, 700);
  };

  return (
    <div id="easter-egg-banner" className="bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-900/50 rounded-2xl p-5 shadow-2xs max-w-md w-full my-4">
      <div className="flex items-center justify-between pb-2 border-b border-amber-200/60 dark:border-amber-900/40 mb-3">
        <span className="text-xs font-bold text-amber-700 dark:text-amber-300 uppercase tracking-wider flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5 text-amber-500" /> Interactive Easter Egg
        </span>
        {onDismiss && (
          <button
            onClick={onDismiss}
            className="text-xs text-amber-700 dark:text-amber-300 hover:underline cursor-pointer"
          >
            Dismiss
          </button>
        )}
      </div>

      {effect === 'barrel_roll' && (
        <div className="text-center py-2">
          <p className="text-sm font-bold text-slate-900 dark:text-white">
            🌀 Wheee! Doing a Barrel Roll!
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Inspired by classic Nintendo Star Fox and Google Search.
          </p>
        </div>
      )}

      {effect === 'askew' && (
        <div className="text-center py-2">
          <p className="text-sm font-bold text-slate-900 dark:text-white">
            📐 Page tilted slightly askew!
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            "Askew" means not in a straight or level position.
          </p>
        </div>
      )}

      {effect === 'coin_flip' && (
        <div className="flex flex-col items-center justify-center py-3">
          <div
            className={`w-24 h-24 rounded-full bg-gradient-to-tr from-amber-400 to-yellow-200 dark:from-amber-600 dark:to-yellow-400 border-4 border-amber-500 dark:border-yellow-300 flex items-center justify-center shadow-lg transition-transform duration-500 ${
              isFlipping ? 'animate-spin scale-110' : ''
            }`}
          >
            <div className="text-center text-amber-900 font-extrabold text-lg tracking-wider">
              {coinResult || '...'}
            </div>
          </div>

          <button
            onClick={flipCoin}
            disabled={isFlipping}
            className="mt-4 px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-semibold flex items-center gap-2 shadow-2xs transition-colors cursor-pointer"
          >
            <Coins className="w-4 h-4" /> Flip Again
          </button>
        </div>
      )}

      {effect === 'roll_dice' && (
        <div className="flex flex-col items-center justify-center py-3">
          <div
            className={`w-20 h-20 rounded-2xl bg-white dark:bg-slate-900 border-2 border-slate-200 dark:border-slate-700 flex items-center justify-center shadow-lg transition-transform duration-300 ${
              isRolling ? 'rotate-180 scale-110' : ''
            }`}
          >
            <span className="text-4xl font-extrabold text-slate-900 dark:text-white">
              {diceResult || '?'}
            </span>
          </div>

          <button
            onClick={rollDice}
            disabled={isRolling}
            className="mt-4 px-4 py-2 bg-slate-900 hover:bg-slate-800 dark:bg-slate-100 dark:hover:bg-white dark:text-slate-900 text-white rounded-xl text-xs font-semibold flex items-center gap-2 shadow-2xs transition-colors cursor-pointer"
          >
            <Dices className="w-4 h-4" /> Roll Again
          </button>
        </div>
      )}
    </div>
  );
};
