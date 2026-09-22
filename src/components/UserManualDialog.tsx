import React, { useState } from 'react';
import { MANUAL_SECTIONS, ManualSection } from '../utils/userManual';
import { BookOpen, Search, X, Printer, Copy, Check } from 'lucide-react';

interface UserManualDialogProps {
  onClose: () => void;
}

export const UserManualDialog: React.FC<UserManualDialogProps> = ({ onClose }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [copied, setCopied] = useState(false);

  const filteredSections = MANUAL_SECTIONS.filter(
    (sec) =>
      sec.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      sec.contents.some((c) => c.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleCopyAll = () => {
    const fullText = MANUAL_SECTIONS.map((sec) => `${sec.title}\n${sec.contents.join('\n')}`).join('\n\n');
    navigator.clipboard.writeText(fullText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-3xl max-h-[85vh] bg-slate-900 border border-slate-800 rounded-3xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-900/90">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <BookOpen className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100">User Operations Manual</h2>
              <p className="text-xs text-slate-400">Comprehensive reference guide for Excel, DAT Vault, and Security.</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCopyAll}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition"
              title="Copy All Manual Text"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
            <button
              onClick={handlePrint}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition"
              title="Print Manual"
            >
              <Printer className="w-4 h-4" />
            </button>
            <button
              onClick={onClose}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className="p-4 border-b border-slate-800/80 bg-slate-950/50">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search user manual topics (e.g. fingerprint, encryption, excel)…"
              className="w-full pl-9 pr-4 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>
        </div>

        {/* Sections list */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {filteredSections.map((sec, idx) => (
            <div
              key={idx}
              className="p-4 bg-slate-950/60 rounded-2xl border border-slate-800/80 hover:border-slate-700 transition"
            >
              <div className="flex items-center gap-2 mb-2 font-bold text-sm text-slate-100">
                <span>{sec.iconEmoji}</span>
                <span>{sec.title}</span>
              </div>
              <div className="space-y-1.5 text-xs text-slate-300 leading-relaxed pl-6">
                {sec.contents.map((paragraph, pIdx) => (
                  <p key={pIdx}>{paragraph}</p>
                ))}
              </div>
            </div>
          ))}

          {filteredSections.length === 0 && (
            <div className="text-center p-8 text-slate-400 text-xs">
              No manual entries matching "{searchTerm}".
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
