import React, { useState, useRef } from 'react';
import { Camera, Upload, Link as LinkIcon, X, Sparkles, Loader2, Search, FileText, Check } from 'lucide-react';
import { analyzeWithLens } from '../services/api';

interface LensModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectQuery: (query: string) => void;
}

export const LensModal: React.FC<LensModalProps> = ({
  isOpen,
  onClose,
  onSelectQuery,
}) => {
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [mimeType, setMimeType] = useState<string>('image/jpeg');
  const [urlInput, setUrlInput] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [lensData, setLensData] = useState<any | null>(null);
  const [copiedText, setCopiedText] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<'upload' | 'url' | 'camera'>('upload');
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setMimeType(file.type || 'image/jpeg');
      const reader = new FileReader();
      reader.onload = (event) => {
        const base64 = event.target?.result as string;
        setImagePreview(base64);
        runLensAnalysis(base64, file.type || 'image/jpeg');
      };
      reader.readAsDataURL(file);
    }
  };

  const handleUrlSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!urlInput.trim()) return;
    setImagePreview(urlInput);
    // For URL, fetch or pass base64
    runLensAnalysis(urlInput, 'image/jpeg', `Search for visual matches for this image URL: ${urlInput}`);
  };

  const startCamera = async () => {
    try {
      setActiveTab('camera');
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      setCameraStream(stream);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch {
      alert('Could not access camera. Please allow camera permissions.');
    }
  };

  const capturePhoto = () => {
    if (!videoRef.current) return;
    const canvas = document.createElement('canvas');
    canvas.width = videoRef.current.videoWidth || 640;
    canvas.height = videoRef.current.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
      const base64 = canvas.toDataURL('image/jpeg');
      setImagePreview(base64);
      setMimeType('image/jpeg');
      stopCamera();
      runLensAnalysis(base64, 'image/jpeg');
    }
  };

  const stopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());
      setCameraStream(null);
    }
  };

  const handleClose = () => {
    stopCamera();
    setImagePreview(null);
    setLensData(null);
    onClose();
  };

  const runLensAnalysis = async (imgBase64: string, type: string, prompt?: string) => {
    setLoading(true);
    setLensData(null);
    try {
      const data = await analyzeWithLens(imgBase64, type, prompt);
      setLensData(data);
    } catch (err: any) {
      alert(err?.message || 'Visual search failed. Please try another image.');
    } finally {
      setLoading(false);
    }
  };

  const handleCopyText = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(true);
    setTimeout(() => setCopiedText(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-3xl w-full overflow-hidden shadow-2xl border border-slate-200 dark:border-slate-800 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-4 px-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-blue-600 text-white shadow-2xs">
              <Camera className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <span>Google Lens</span>
                <span className="text-[10px] font-semibold bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full">
                  Visual Search
                </span>
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Search any image with visual AI and real-time web grounding
              </p>
            </div>
          </div>

          <button
            onClick={handleClose}
            className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto flex-1">
          {!imagePreview && activeTab !== 'camera' ? (
            <div className="space-y-6">
              {/* Drag & Drop Area */}
              <div
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  const file = e.dataTransfer.files?.[0];
                  if (file) {
                    setMimeType(file.type || 'image/jpeg');
                    const reader = new FileReader();
                    reader.onload = (event) => {
                      const base64 = event.target?.result as string;
                      setImagePreview(base64);
                      runLensAnalysis(base64, file.type || 'image/jpeg');
                    };
                    reader.readAsDataURL(file);
                  }
                }}
                className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-blue-500 dark:hover:border-blue-400 rounded-2xl p-8 text-center cursor-pointer transition-all bg-slate-50/50 dark:bg-slate-950/50 hover:bg-blue-50/20"
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept="image/*"
                  className="hidden"
                />
                <div className="w-14 h-14 mx-auto rounded-full bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center mb-3">
                  <Upload className="w-6 h-6" />
                </div>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                  Drag an image here or <span className="text-blue-600 dark:text-blue-400 underline">upload a file</span>
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                  Supports JPG, PNG, WEBP, GIF
                </p>
              </div>

              {/* Alternative Options */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  onClick={startCamera}
                  className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 flex items-center justify-center gap-2 text-xs font-semibold text-slate-800 dark:text-slate-200 transition-colors cursor-pointer"
                >
                  <Camera className="w-4 h-4 text-emerald-500" />
                  Take a photo with camera
                </button>

                <div className="relative">
                  <form onSubmit={handleUrlSubmit} className="flex">
                    <input
                      type="url"
                      placeholder="Paste image URL..."
                      value={urlInput}
                      onChange={(e) => setUrlInput(e.target.value)}
                      className="w-full text-xs px-3 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl focus:outline-hidden focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                    />
                    <button
                      type="submit"
                      className="absolute right-1.5 top-1.5 px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold cursor-pointer"
                    >
                      Search
                    </button>
                  </form>
                </div>
              </div>
            </div>
          ) : activeTab === 'camera' ? (
            <div className="flex flex-col items-center space-y-4">
              <div className="relative rounded-2xl overflow-hidden bg-black max-w-md w-full aspect-4/3">
                <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover" />
              </div>
              <div className="flex gap-3">
                <button
                  onClick={capturePhoto}
                  className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-full flex items-center gap-2 cursor-pointer shadow-md"
                >
                  <Camera className="w-4 h-4" /> Capture Photo
                </button>
                <button
                  onClick={() => {
                    stopCamera();
                    setActiveTab('upload');
                  }}
                  className="px-4 py-2.5 bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200 text-xs font-semibold rounded-full cursor-pointer"
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            /* Results View */
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Image Preview Side */}
              <div>
                <div className="relative rounded-2xl overflow-hidden bg-slate-100 dark:bg-slate-800 aspect-square flex items-center justify-center border border-slate-200 dark:border-slate-700">
                  <img
                    src={imagePreview}
                    alt="Lens Target"
                    className="max-h-full max-w-full object-contain"
                  />
                  {loading && (
                    <div className="absolute inset-0 bg-slate-950/70 backdrop-blur-xs flex flex-col items-center justify-center text-white p-4 text-center">
                      <Loader2 className="w-8 h-8 animate-spin text-blue-400 mb-2" />
                      <p className="text-xs font-semibold">Analyzing image with Google Lens...</p>
                      <p className="text-[10px] text-slate-300 mt-1">Detecting objects, text, and visual matches</p>
                    </div>
                  )}
                </div>

                <div className="mt-3 flex justify-between">
                  <button
                    onClick={() => {
                      setImagePreview(null);
                      setLensData(null);
                      setActiveTab('upload');
                    }}
                    className="text-xs text-blue-600 dark:text-blue-400 hover:underline font-semibold cursor-pointer"
                  >
                    ← Upload different image
                  </button>
                </div>
              </div>

              {/* Lens Results Side */}
              <div className="space-y-4">
                {lensData ? (
                  <>
                    <div className="pb-3 border-b border-slate-100 dark:border-slate-800">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60 px-2 py-0.5 rounded-md">
                        {lensData.category || 'Visual Match'}
                      </span>
                      <h3 className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                        {lensData.entityTitle}
                      </h3>
                      <p className="text-xs text-slate-600 dark:text-slate-300 mt-1 leading-relaxed">
                        {lensData.description}
                      </p>
                    </div>

                    {/* OCR Text if detected */}
                    {lensData.extractedText && (
                      <div className="bg-amber-50/70 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-900/50 rounded-xl p-3">
                        <div className="flex items-center justify-between mb-1.5">
                          <span className="text-xs font-bold text-amber-800 dark:text-amber-300 flex items-center gap-1">
                            <FileText className="w-3.5 h-3.5" /> Detected Text (OCR)
                          </span>
                          <button
                            onClick={() => handleCopyText(lensData.extractedText)}
                            className="text-[11px] text-amber-700 dark:text-amber-300 hover:underline flex items-center gap-1 cursor-pointer"
                          >
                            {copiedText ? <Check className="w-3 h-3" /> : 'Copy text'}
                          </button>
                        </div>
                        <p className="text-xs font-mono text-slate-800 dark:text-slate-200 bg-white/80 dark:bg-slate-900 p-2 rounded-lg break-words">
                          {lensData.extractedText}
                        </p>
                      </div>
                    )}

                    {/* Key Attributes */}
                    {lensData.keyAttributes && lensData.keyAttributes.length > 0 && (
                      <div className="space-y-1 text-xs">
                        {lensData.keyAttributes.map((attr: any, idx: number) => (
                          <div key={idx} className="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                            <span className="text-slate-500 font-medium">{attr.label}</span>
                            <span className="text-slate-900 dark:text-white font-semibold">{attr.value}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Suggested Searches */}
                    {lensData.suggestedQueries && lensData.suggestedQueries.length > 0 && (
                      <div className="pt-2">
                        <span className="text-xs font-bold text-slate-700 dark:text-slate-300 block mb-2">
                          Explore Further:
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {lensData.suggestedQueries.map((q: string, idx: number) => (
                            <button
                              key={idx}
                              onClick={() => {
                                handleClose();
                                onSelectQuery(q);
                              }}
                              className="text-xs bg-blue-50 dark:bg-blue-950/50 hover:bg-blue-100 dark:hover:bg-blue-900/50 text-blue-700 dark:text-blue-300 border border-blue-200/60 dark:border-blue-800 px-3 py-1.5 rounded-full flex items-center gap-1.5 transition-colors cursor-pointer"
                            >
                              <Search className="w-3 h-3" />
                              <span>{q}</span>
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </>
                ) : !loading ? (
                  <div className="py-8 text-center text-slate-500">
                    <Sparkles className="w-8 h-8 mx-auto text-slate-400 mb-2" />
                    <p className="text-xs">Select an image above to begin visual recognition.</p>
                  </div>
                ) : null}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
