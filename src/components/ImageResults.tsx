import React, { useState } from 'react';
import { ExternalLink, X, Image as ImageIcon, ZoomIn } from 'lucide-react';
import { ImageResultItem } from '../types';

interface ImageResultsProps {
  images: ImageResultItem[];
}

export const ImageResults: React.FC<ImageResultsProps> = ({ images }) => {
  const [selectedImage, setSelectedImage] = useState<ImageResultItem | null>(null);

  if (!images || images.length === 0) {
    return (
      <div className="py-16 text-center text-gray-500 dark:text-gray-400">
        <ImageIcon className="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-2" />
        <p className="text-base font-medium">No images found for this search.</p>
      </div>
    );
  }

  return (
    <div id="image-results-grid">
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5">
        {images.map((img) => {
          const fallbackUrl = `https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.0.3`;
          return (
            <div
              key={img.id || img.imageUrl}
              onClick={() => setSelectedImage(img)}
              className="group relative bg-white dark:bg-slate-900 rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 hover:shadow-md transition-all cursor-pointer flex flex-col justify-between shadow-2xs"
            >
              <div className="aspect-4/3 overflow-hidden relative bg-slate-100 dark:bg-slate-800">
                <img
                  src={img.imageUrl || fallbackUrl}
                  alt={img.title}
                  loading="lazy"
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src = fallbackUrl;
                  }}
                />
                <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white backdrop-blur-[2px]">
                  <ZoomIn className="w-6 h-6" />
                </div>
              </div>

              <div className="p-2.5 bg-white dark:bg-slate-900">
                <h4 className="text-xs font-semibold text-slate-900 dark:text-slate-100 truncate">
                  {img.title}
                </h4>
                <p className="text-[11px] text-slate-400 dark:text-slate-400 truncate mt-0.5 font-medium">
                  {img.sourceDomain || 'web'}
                </p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Lightbox Modal */}
      {selectedImage && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl max-w-4xl w-full overflow-hidden shadow-2xl flex flex-col md:flex-row border border-slate-200 dark:border-slate-800 max-h-[90vh]">
            {/* Image side */}
            <div className="flex-1 bg-slate-950 flex items-center justify-center p-4 min-h-[300px] max-h-[60vh] md:max-h-full">
              <img
                src={selectedImage.imageUrl}
                alt={selectedImage.title}
                className="max-h-full max-w-full object-contain rounded-lg shadow-md"
                onError={(e) => {
                  (e.target as HTMLImageElement).src =
                    'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80';
                }}
              />
            </div>

            {/* Info side */}
            <div className="w-full md:w-80 p-5 flex flex-col justify-between border-t md:border-t-0 md:border-l border-slate-200 dark:border-slate-800">
              <div>
                <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Image Details
                  </span>
                  <button
                    onClick={() => setSelectedImage(null)}
                    className="p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 cursor-pointer transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="mt-4 space-y-2">
                  <h3 className="text-base font-bold text-slate-900 dark:text-white leading-snug">
                    {selectedImage.title}
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Source: <strong className="text-slate-800 dark:text-slate-200">{selectedImage.sourceDomain}</strong>
                  </p>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 mt-6 space-y-2">
                <a
                  href={selectedImage.sourceUrl || selectedImage.imageUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer shadow-2xs"
                >
                  <span>Visit Website</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>

                <a
                  href={selectedImage.imageUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-2.5 px-4 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 rounded-xl text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer border border-slate-200/80 dark:border-slate-700/80"
                >
                  <span>Open Full Image</span>
                </a>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
