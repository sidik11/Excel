import React from 'react';
import { Database, FolderCheck, BookOpen, User, Bell, Trash2, Sparkles, ShieldCheck } from 'lucide-react';
import { UserProfile } from '../types';

interface HeaderBarProps {
  excelName: string | null;
  folderName: string | null;
  isPermanentSaved: boolean;
  userProfile: UserProfile | null;
  customLogoUrl?: string;
  onLoadSample: () => void;
  onTestNotification: () => void;
  onClearCache: () => void;
  onOpenManual: () => void;
  onOpenProfile: () => void;
}

export const HeaderBar: React.FC<HeaderBarProps> = ({
  excelName,
  folderName,
  isPermanentSaved,
  userProfile,
  customLogoUrl,
  onLoadSample,
  onTestNotification,
  onClearCache,
  onOpenManual,
  onOpenProfile,
}) => {
  const isProfileComplete = userProfile?.fullName && userProfile?.isGoogleConnected;

  return (
    <header className="w-full bg-slate-900/90 backdrop-blur-md border-b border-slate-800 px-4 py-3 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Logo and Brand */}
        <div className="flex items-center gap-3 w-full md:w-auto justify-between md:justify-start">
          <div className="flex items-center gap-3">
            {customLogoUrl ? (
              <img
                src={customLogoUrl}
                alt="App Logo"
                className="w-9 h-9 rounded-xl object-cover ring-2 ring-indigo-500/50"
              />
            ) : (
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 via-purple-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-500/20 text-white font-black text-lg">
                V
              </div>
            )}
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-bold text-slate-100 tracking-tight">
                  Excel & Image Vault
                </h1>
                <span className="text-[10px] px-1.5 py-0.5 rounded font-mono font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  v1.0.1
                </span>
              </div>
              <p className="text-xs text-slate-400 hidden sm:block">
                Catalog Search & Encrypted Image Vault
              </p>
            </div>
          </div>

          {/* Mobile Profile & Manual Quick Action */}
          <div className="flex md:hidden items-center gap-1.5">
            <button
              onClick={onOpenManual}
              className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition"
              title="User Manual"
            >
              <BookOpen className="w-4 h-4" />
            </button>
            <button
              onClick={onOpenProfile}
              className="relative p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition"
              title="Profile"
            >
              <User className="w-4 h-4" />
              {!isProfileComplete && (
                <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-amber-500 animate-ping" />
              )}
            </button>
          </div>
        </div>

        {/* Status Badges */}
        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto justify-center md:justify-start text-xs font-mono">
          <div
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border ${
              excelName
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                : 'bg-slate-800/80 border-slate-700 text-slate-400'
            }`}
          >
            <Database className="w-3.5 h-3.5" />
            <span className="max-w-[130px] truncate">
              {excelName || 'No Excel Loaded'}
            </span>
          </div>

          <div
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border ${
              folderName
                ? 'bg-cyan-500/10 border-cyan-500/30 text-cyan-400'
                : 'bg-slate-800/80 border-slate-700 text-slate-400'
            }`}
          >
            <FolderCheck className="w-3.5 h-3.5" />
            <span className="max-w-[130px] truncate">
              {folderName || 'No Folder Loaded'}
            </span>
          </div>

          {isPermanentSaved && (
            <div className="hidden lg:flex items-center gap-1 px-2 py-0.5 rounded text-[11px] bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <ShieldCheck className="w-3 h-3 text-indigo-400" />
              <span>Synced</span>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2 w-full md:w-auto justify-end">
          <button
            onClick={onLoadSample}
            id="btn_load_sample_data"
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white shadow-sm transition active:scale-95"
            title="Load sample dress/apparel catalog with instant preview images"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Sample Catalog</span>
          </button>

          <button
            onClick={onTestNotification}
            id="btn_test_notification"
            className="hidden sm:flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
            title="Trigger test alert"
          >
            <Bell className="w-3.5 h-3.5" />
            <span>Test Alert</span>
          </button>

          <button
            onClick={onClearCache}
            id="btn_clear_cache"
            className="p-1.5 rounded-lg bg-slate-800 hover:bg-red-500/20 hover:text-red-400 text-slate-400 transition"
            title="Clear all cached data"
          >
            <Trash2 className="w-4 h-4" />
          </button>

          <div className="hidden md:flex items-center gap-2 border-l border-slate-800 pl-2">
            <button
              onClick={onOpenManual}
              id="btn_open_user_manual"
              className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
            >
              <BookOpen className="w-3.5 h-3.5 text-indigo-400" />
              <span>Manual</span>
            </button>

            <button
              onClick={onOpenProfile}
              id="btn_open_profile"
              className={`flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium rounded-lg border transition ${
                isProfileComplete
                  ? 'bg-slate-800 border-slate-700 text-slate-200 hover:bg-slate-700'
                  : 'bg-amber-500/10 border-amber-500/30 text-amber-300 animate-pulse'
              }`}
            >
              {userProfile?.profileImageBase64 ? (
                <img
                  src={userProfile.profileImageBase64}
                  alt="Avatar"
                  className="w-4 h-4 rounded-full object-cover"
                />
              ) : (
                <User className="w-3.5 h-3.5 text-amber-400" />
              )}
              <span>{userProfile?.fullName ? userProfile.fullName.split(' ')[0] : 'Profile'}</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
