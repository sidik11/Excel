import React from 'react';
import { MainAppTab } from '../types';
import { Table, ShieldAlert, Sparkles, Network, Settings, Lock } from 'lucide-react';

interface TabNavigationProps {
  currentTab: MainAppTab;
  isDualConnected: boolean;
  isPinEnabled: boolean;
  onTabChange: (tab: MainAppTab) => void;
  onQuickLock: () => void;
}

export const TabNavigation: React.FC<TabNavigationProps> = ({
  currentTab,
  isDualConnected,
  isPinEnabled,
  onTabChange,
  onQuickLock,
}) => {
  return (
    <nav className="w-full bg-slate-900/60 border-b border-slate-800 px-4 py-2 sticky top-[61px] z-20 backdrop-blur">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-2 overflow-x-auto no-scrollbar">
        <div className="flex items-center gap-1.5 p-1 bg-slate-950/60 rounded-xl border border-slate-800/80">
          {/* Excel Tab */}
          <button
            onClick={() => onTabChange('EXCEL_CATALOG')}
            id="nav_excel_catalog_tab"
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              currentTab === 'EXCEL_CATALOG'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Table className="w-3.5 h-3.5" />
            <span>📊 Excel</span>
          </button>

          {/* DAT Vault Tab */}
          <button
            onClick={() => onTabChange('IMAGE_VAULT')}
            id="nav_image_vault_tab"
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              currentTab === 'IMAGE_VAULT'
                ? 'bg-gradient-to-r from-purple-600 to-cyan-500 text-white shadow-md shadow-purple-600/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <ShieldAlert className="w-3.5 h-3.5" />
            <span>🔐 DAT Vault</span>
          </button>

          {/* SShow Tab */}
          <button
            onClick={() => onTabChange('SSHOW')}
            id="nav_sshow_tab"
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              currentTab === 'SSHOW'
                ? 'bg-gradient-to-r from-rose-500 to-amber-500 text-white shadow-md shadow-rose-500/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>🛡️ SShow</span>
          </button>

          {/* Dual Vault Tab (always selectable or highlighted when connected) */}
          <button
            onClick={() => onTabChange('DUAL_VAULT')}
            id="nav_dual_vault_tab"
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              currentTab === 'DUAL_VAULT'
                ? 'bg-gradient-to-r from-emerald-500 to-sky-600 text-white shadow-md shadow-emerald-500/20'
                : isDualConnected
                ? 'text-emerald-400 bg-emerald-500/10 border border-emerald-500/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Network className="w-3.5 h-3.5" />
            <span>🔗 Dual Vault</span>
            {isDualConnected && (
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            )}
          </button>

          {/* Settings Tab */}
          <button
            onClick={() => onTabChange('SETTINGS')}
            id="nav_settings_tab"
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
              currentTab === 'SETTINGS'
                ? 'bg-slate-700 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Settings className="w-3.5 h-3.5" />
            <span>⚙️ Settings</span>
          </button>
        </div>

        {/* Quick Lock */}
        {isPinEnabled && (
          <button
            onClick={onQuickLock}
            id="nav_quick_lock_app"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold bg-red-500/15 border border-red-500/30 text-red-400 hover:bg-red-500/25 transition active:scale-95 whitespace-nowrap"
            title="Lock application immediately"
          >
            <Lock className="w-3.5 h-3.5" />
            <span>Lock</span>
          </button>
        )}
      </div>
    </nav>
  );
};
