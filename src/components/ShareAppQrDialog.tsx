import React, { useState } from 'react';
import { Share2, Copy, Check, X, QrCode, Globe } from 'lucide-react';

interface ShareAppQrDialogProps {
  onClose: () => void;
}

export const ShareAppQrDialog: React.FC<ShareAppQrDialogProps> = ({ onClose }) => {
  const [copied, setCopied] = useState(false);
  const currentUrl = window.location.href;

  const handleCopyLink = () => {
    navigator.clipboard.writeText(currentUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // QR code SVG via public api or SVG render
  const qrApiUrl = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(
    currentUrl
  )}&bgcolor=0f172a&color=e2e8f0`;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col items-center gap-4 text-center">
        <div className="w-full flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <Share2 className="w-4 h-4 text-cyan-400" />
            <h3 className="text-sm font-bold text-slate-100">Share Excel & Image Vault</h3>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* QR Code */}
        <div className="p-4 bg-slate-950 rounded-2xl border border-slate-800 shadow-inner">
          <img
            src={qrApiUrl}
            alt="App QR Code"
            className="w-48 h-48 rounded-xl object-contain"
            onError={(e) => {
              // fallback if offline
              e.currentTarget.style.display = 'none';
            }}
          />
        </div>

        <p className="text-xs text-slate-400">
          Scan this QR Code on your mobile phone or tablet to open this application instantly.
        </p>

        {/* Copy Link */}
        <div className="w-full flex items-center gap-2 bg-slate-950 p-2 rounded-xl border border-slate-800">
          <input
            type="text"
            readOnly
            value={currentUrl}
            className="flex-1 bg-transparent text-xs text-slate-300 font-mono px-2 outline-none truncate"
          />
          <button
            onClick={handleCopyLink}
            id="btn_copy_share_url"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition active:scale-95"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-white" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied' : 'Copy'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
