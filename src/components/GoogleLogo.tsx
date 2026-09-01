import React from 'react';

interface GoogleLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  onClick?: () => void;
  className?: string;
  animate?: boolean;
}

export const GoogleLogo: React.FC<GoogleLogoProps> = ({
  size = 'md',
  onClick,
  className = '',
  animate = false,
}) => {
  const letters = [
    { char: 'G', color: 'text-[#4285F4]' },
    { char: 'o', color: 'text-[#EA4335]' },
    { char: 'o', color: 'text-[#FBBC05]' },
    { char: 'g', color: 'text-[#4285F4]' },
    { char: 'l', color: 'text-[#34A853]' },
    { char: 'e', color: 'text-[#EA4335]' },
  ];

  const sizeClasses = {
    sm: 'text-2xl tracking-tighter',
    md: 'text-4xl tracking-tight',
    lg: 'text-6xl md:text-7xl tracking-tight',
    xl: 'text-7xl md:text-8xl tracking-tight',
  };

  return (
    <div
      id="google-logo-container"
      onClick={onClick}
      className={`inline-flex items-center select-none font-medium transition-transform duration-200 cursor-pointer ${
        sizeClasses[size]
      } ${className}`}
      style={{ fontFamily: '"Product Sans", "Futura", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif' }}
    >
      {letters.map((item, idx) => (
        <span
          key={idx}
          className={`${item.color} font-bold inline-block transition-transform hover:-translate-y-1 duration-150 ${
            animate ? 'animate-bounce' : ''
          }`}
          style={{ animationDelay: `${idx * 80}ms` }}
        >
          {item.char}
        </span>
      ))}
      {size === 'lg' || size === 'xl' ? (
        <span className="text-xs font-semibold uppercase tracking-widest text-emerald-600 dark:text-emerald-400 bg-emerald-100 dark:bg-emerald-950/60 px-2 py-0.5 rounded-full ml-2 self-start -mt-1 shadow-xs border border-emerald-300 dark:border-emerald-800">
          AI Powered
        </span>
      ) : null}
    </div>
  );
};
