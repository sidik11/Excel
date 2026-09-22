import React, { useState, useEffect, useCallback } from 'react';
import { VaultImage } from '../types';
import { X, ChevronLeft, ChevronRight, Play, Pause, ZoomIn, ZoomOut, RotateCcw, Download } from 'lucide-react';

interface VaultModalViewerProps {
  images: VaultImage[];
  currentIndex: number;
  onClose: () => void;
  onNext: () => void;
  onPrevious: () => void;
}

export const VaultModalViewer: React.FC<VaultModalViewerProps> = ({
  images,
  currentIndex,
  onClose,
  onNext,
  onPrevious,
}) => {
  const [scale, setScale] = useState(1);
  const [isPlaying, setIsPlaying] = useState(false);

  const currentImage = images[currentIndex];

  const handleZoomIn = () => setScale(prev => Math.min(prev + 0.3, 3));
  const handleZoomOut = () => setScale(prev => Math.max(prev - 0.3, 0.5));
  const handleResetZoom = () => setScale(1);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight') onNext();
      if (e.key === 'ArrowLeft') onPrevious();
      if (e.key === 'Escape') onClose();
      if (e.key === ' ') {
        e.preventDefault();
        setIsPlaying(prev => !prev);
      }
    },
    [onNext, onPrevious, onClose]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  useEffect(() => {
    if (!isPlaying) return;
    const timer = setInterval(() => {
      onNext();
    }, 1500);
    return () => clearInterval(timer);
  }, [isPlaying, onNext]);

  useEffect(() => {
    setScale(1);
  }, [currentIndex]);

  const handleDownload = () => {
    if (!currentImage) return;
    const a = document.createElement('a');
    a.href = currentImage.dataUrl;
    a.download = currentImage.name;
    a.click();
  };

  if (!currentImage) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/95 flex flex-col justify-between backdrop-blur-md select-none">
      {/* Top Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-slate-900/80 border-b border-slate-800 z-10">
        <div className="flex items-center gap-3">
          <span className="font-mono text-xs px-2.5 py-1 rounded-md bg-purple-500/20 text-purple-300 border border-purple-500/30">
            {currentIndex + 1} / {images.length}
          </span>
          <span className="text-sm font-semibold text-slate-200 truncate max-w-xs sm:max-w-md">
            {currentImage.name}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <div className="hidden sm:flex items-center gap-1 bg-slate-800 rounded-lg p-1 border border-slate-700">
            <button
              onClick={handleZoomOut}
              className="p-1.5 text-slate-300 hover:text-white rounded hover:bg-slate-700"
              title="Zoom Out"
            >
              <ZoomOut className="w-4 h-4" />
            </button>
            <span className="text-xs font-mono text-slate-400 px-1">{Math.round(scale * 100)}%</span>
            <button
              onClick={handleZoomIn}
              className="p-1.5 text-slate-300 hover:text-white rounded hover:bg-slate-700"
              title="Zoom In"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
            {scale !== 1 && (
              <button
                onClick={handleResetZoom}
                className="p-1.5 text-slate-300 hover:text-white rounded hover:bg-slate-700"
              >
                <RotateCcw className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <button
            onClick={handleDownload}
            className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white border border-slate-700 transition"
            title="Download image"
          >
            <Download className="w-4 h-4" />
          </button>

          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-slate-800 hover:bg-red-500/20 hover:text-red-400 text-slate-300 border border-slate-700 transition"
            title="Close Lightbox"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Main Image Stage */}
      <div className="relative flex-1 flex items-center justify-center p-4 overflow-hidden">
        <button
          onClick={onPrevious}
          className="absolute left-4 top-1/2 -translate-y-1/2 p-3 rounded-2xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 shadow-2xl z-20 transition active:scale-95"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>

        <button
          onClick={onNext}
          className="absolute right-4 top-1/2 -translate-y-1/2 p-3 rounded-2xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 shadow-2xl z-20 transition active:scale-95"
        >
          <ChevronRight className="w-6 h-6" />
        </button>

        <div className="max-w-full max-h-full flex items-center justify-center">
          <img
            src={currentImage.dataUrl}
            alt={currentImage.name}
            style={{ transform: `scale(${scale})` }}
            className="max-w-[90vw] max-h-[75vh] object-contain rounded-xl shadow-2xl transition-transform"
          />
        </div>
      </div>

      {/* Bottom Controls */}
      <div className="flex items-center justify-center gap-3 px-4 py-3 bg-slate-900/80 border-t border-slate-800 z-10">
        <button
          onClick={() => setIsPlaying(!isPlaying)}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold shadow-lg transition active:scale-95 ${
            isPlaying ? 'bg-amber-600 text-white' : 'bg-purple-600 hover:bg-purple-500 text-white'
          }`}
        >
          {isPlaying ? (
            <>
              <Pause className="w-4 h-4" />
              <span>Pause Slideshow</span>
            </>
          ) : (
            <>
              <Play className="w-4 h-4" />
              <span>Play Slideshow</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
