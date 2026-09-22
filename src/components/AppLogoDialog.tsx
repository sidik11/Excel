import React, { useRef } from 'react';
import { Image as ImageIcon, Upload, X, Check } from 'lucide-react';

interface AppLogoDialogProps {
  currentLogoUrl?: string;
  onSaveLogo: (logoUrl: string) => void;
  onClose: () => void;
}

const PRESET_LOGOS = [
  'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=128&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=128&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1618005198919-d3d4b5a92ead?w=128&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=128&auto=format&fit=crop&q=80'
];

export const AppLogoDialog: React.FC<AppLogoDialogProps> = ({
  currentLogoUrl,
  onSaveLogo,
  onClose,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleUploadCustom = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        onSaveLogo(reader.result as string);
        onClose();
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <ImageIcon className="w-4 h-4 text-purple-400" />
            <h3 className="text-sm font-bold text-slate-100">Custom App Branding Logo</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-xs text-slate-400">
          Upload your personal or business emblem to brand the header and app icons.
        </p>

        {/* Current / Upload button */}
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleUploadCustom}
          accept="image/*"
          className="hidden"
          id="input_custom_app_logo"
        />

        <button
          onClick={() => fileInputRef.current?.click()}
          id="btn_upload_custom_logo"
          className="flex items-center justify-center gap-2 py-3 px-4 rounded-2xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs transition active:scale-95 shadow-md shadow-indigo-600/20"
        >
          <Upload className="w-4 h-4" />
          <span>Upload Custom Logo File</span>
        </button>

        {/* Presets */}
        <div className="flex flex-col gap-2 pt-2 border-t border-slate-800">
          <span className="text-[11px] font-semibold text-slate-400">Or choose a preset style:</span>
          <div className="grid grid-cols-4 gap-3">
            {PRESET_LOGOS.map((url, idx) => (
              <button
                key={idx}
                onClick={() => {
                  onSaveLogo(url);
                  onClose();
                }}
                className={`relative aspect-square rounded-xl overflow-hidden border-2 transition ${
                  currentLogoUrl === url
                    ? 'border-indigo-500 ring-2 ring-indigo-500/40'
                    : 'border-slate-800 hover:border-slate-600'
                }`}
              >
                <img src={url} alt={`Preset ${idx + 1}`} className="w-full h-full object-cover" />
                {currentLogoUrl === url && (
                  <div className="absolute inset-0 bg-indigo-600/40 flex items-center justify-center">
                    <Check className="w-4 h-4 text-white" />
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
