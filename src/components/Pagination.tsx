import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  currentPage: number;
  totalPages?: number;
  onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage = 1,
  totalPages = 10,
  onPageChange,
}) => {
  const pages = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  return (
    <div id="search-pagination" className="my-10 flex flex-col items-center justify-center select-none">
      {/* Google Logo styled Page letters */}
      <div className="flex items-center text-3xl font-extrabold tracking-tighter mb-2" style={{ fontFamily: '"Product Sans", -apple-system, sans-serif' }}>
        <span className="text-[#4285F4]">G</span>
        {pages.map((p) => {
          const isSelected = p === currentPage;
          return (
            <span
              key={p}
              onClick={() => onPageChange(p)}
              className={`cursor-pointer transition-transform hover:scale-125 px-0.5 ${
                isSelected ? 'text-[#EA4335]' : 'text-[#FBBC05]'
              }`}
            >
              o
            </span>
          );
        })}
        <span className="text-[#4285F4]">g</span>
        <span className="text-[#34A853]">l</span>
        <span className="text-[#EA4335]">e</span>
      </div>

      {/* Page Numbers */}
      <div className="flex items-center gap-1.5 text-xs sm:text-sm font-semibold">
        {currentPage > 1 && (
          <button
            onClick={() => onPageChange(currentPage - 1)}
            className="px-3 py-1.5 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-xl flex items-center gap-1 cursor-pointer mr-1 font-medium transition-colors"
          >
            <ChevronLeft className="w-4 h-4" /> Previous
          </button>
        )}

        {pages.map((p) => {
          const isSelected = p === currentPage;
          return (
            <button
              key={p}
              onClick={() => onPageChange(p)}
              className={`w-8 h-8 rounded-xl flex items-center justify-center transition-colors cursor-pointer text-xs font-semibold ${
                isSelected
                  ? 'bg-blue-600 text-white shadow-2xs font-bold'
                  : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              {p}
            </button>
          );
        })}

        {currentPage < totalPages && (
          <button
            onClick={() => onPageChange(currentPage + 1)}
            className="px-3 py-1.5 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-xl flex items-center gap-1 cursor-pointer ml-1 font-medium transition-colors"
          >
            Next <ChevronRight className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
};
