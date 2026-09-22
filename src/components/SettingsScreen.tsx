import React, { useState, useRef } from 'react';
import { AppSettings, UserProfile } from '../types';
import { generateFingerprintBackupDat, verifyFingerprintBackupDat } from '../utils/crypto';
import {
  Settings,
  Shield,
  Fingerprint,
  Lock,
  Eye,
  Sliders,
  Smartphone,
  BookOpen,
  Share2,
  Trash2,
  Download,
  Upload,
  Check,
  Image as ImageIcon,
  KeyRound,
  FileCheck2,
  Sparkles
} from 'lucide-react';

interface SettingsScreenProps {
  settings: AppSettings;
  userProfile: UserProfile | null;
  onUpdateSettings: (settings: AppSettings) => void;
  onOpenManual: () => void;
  onOpenShareQr: () => void;
  onOpenLogoPicker: () => void;
  onClearCache: () => void;
  onOpenProfile: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  userProfile,
  onUpdateSettings,
  onOpenManual,
  onOpenShareQr,
  onOpenLogoPicker,
  onClearCache,
  onOpenProfile,
}) => {
  const [pinChangeModal, setPinChangeModal] = useState(false);
  const [currentPinInput, setCurrentPinInput] = useState('');
  const [newPinInput, setNewPinInput] = useState('');
  const [confirmPinInput, setConfirmPinInput] = useState('');
  const [pinError, setPinError] = useState<string | null>(null);
  const [pinSuccess, setPinSuccess] = useState(false);

  const importFingerprintRef = useRef<HTMLInputElement>(null);

  // Toggle PIN lock
  const handleTogglePin = () => {
    if (!settings.isPinLockEnabled) {
      // Enabling PIN
      setPinChangeModal(true);
    } else {
      // Disabling PIN
      const current = prompt('Enter your current PIN to disable app lock:');
      if (current === settings.pinHash) {
        onUpdateSettings({ ...settings, isPinLockEnabled: false });
        alert('App Lock disabled.');
      } else {
        alert('Incorrect PIN.');
      }
    }
  };

  const handleSaveNewPin = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPinInput.length !== 6 || !/^\d+$/.test(newPinInput)) {
      setPinError('PIN must be exactly 6 digits.');
      return;
    }
    if (newPinInput !== confirmPinInput) {
      setPinError('New PINs do not match.');
      return;
    }

    onUpdateSettings({
      ...settings,
      isPinLockEnabled: true,
      pinHash: newPinInput,
    });
    setPinSuccess(true);
    setTimeout(() => {
      setPinSuccess(false);
      setPinChangeModal(false);
      setNewPinInput('');
      setConfirmPinInput('');
      setCurrentPinInput('');
    }, 1500);
  };

  // Register Fingerprint & Generate Backup File
  const handleRegisterFingerprint = async () => {
    const backupContent = generateFingerprintBackupDat(
      userProfile?.fullName || 'VaultUser',
      settings.pinHash || '123456'
    );

    onUpdateSettings({
      ...settings,
      fingerprintRegistered: true,
      fingerprintBackupDatBase64: backupContent,
    });

    alert(
      'Fingerprint credential registered successfully! An emergency backup file (fingerprint_backup.dat) has been generated and securely saved in your settings.'
    );
  };

  // Export fingerprint_backup.dat file
  const handleExportFingerprintDat = () => {
    const datContent =
      settings.fingerprintBackupDatBase64 ||
      generateFingerprintBackupDat(userProfile?.fullName || 'VaultUser', settings.pinHash || '123456');

    const blob = new Blob([datContent], { type: 'application/octet-stream' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'fingerprint_backup.dat';
    a.click();
    URL.revokeObjectURL(url);
  };

  // Import fingerprint_backup.dat
  const handleImportFingerprintDat = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || !e.target.files[0]) return;
    const file = e.target.files[0];
    const reader = new FileReader();

    reader.onload = () => {
      const content = reader.result as string;
      const parsed = verifyFingerprintBackupDat(content);
      if (parsed) {
        onUpdateSettings({
          ...settings,
          fingerprintRegistered: true,
          fingerprintBackupDatBase64: content,
          pinHash: parsed.securityPin,
          isPinLockEnabled: true,
        });
        alert(
          `Verified fingerprint backup for: ${parsed.userIdentifier}.\nEmergency unlock PIN updated.`
        );
      } else {
        alert('Invalid or corrupted fingerprint_backup.dat file.');
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  return (
    <div className="flex-1 flex flex-col bg-slate-950 p-4 md:p-6 overflow-y-auto">
      <div className="max-w-4xl mx-auto w-full flex flex-col gap-6">
        {/* Title */}
        <div className="flex items-center justify-between bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-slate-300">
              <Settings className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-100">Application Settings</h2>
              <p className="text-xs text-slate-400">Configure visual display, security locks, and biometric backup protocols.</p>
            </div>
          </div>
        </div>

        {/* Section 1: Security & Lockscreen */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
            <Shield className="w-4 h-4 text-indigo-400" />
            <h3 className="text-sm font-bold text-slate-200">Security & Protection</h3>
          </div>

          {/* 6-Digit PIN App Lock */}
          <div className="flex items-center justify-between py-2 border-b border-slate-800/60">
            <div>
              <div className="text-xs font-semibold text-slate-200">6-Digit PIN Lockscreen</div>
              <div className="text-[11px] text-slate-400">Enforce PIN entry on launch and wake</div>
            </div>
            <div className="flex items-center gap-2">
              {settings.isPinLockEnabled && (
                <button
                  onClick={() => setPinChangeModal(true)}
                  className="px-2.5 py-1 text-xs rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
                >
                  Change PIN
                </button>
              )}
              <button
                onClick={handleTogglePin}
                className={`w-11 h-6 flex items-center rounded-full p-1 transition duration-300 cursor-pointer ${
                  settings.isPinLockEnabled ? 'bg-indigo-600' : 'bg-slate-800'
                }`}
              >
                <div
                  className={`bg-white w-4 h-4 rounded-full shadow-md transform transition duration-300 ${
                    settings.isPinLockEnabled ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>
          </div>

          {/* Anti-Screenshot & Screen Privacy */}
          <div className="flex items-center justify-between py-2 border-b border-slate-800/60">
            <div>
              <div className="text-xs font-semibold text-slate-200">Anti-Screenshot & Window Masking</div>
              <div className="text-[11px] text-slate-400">Blurs sensitive catalog images when window loses focus</div>
            </div>
            <button
              onClick={() =>
                onUpdateSettings({ ...settings, antiScreenshotProtection: !settings.antiScreenshotProtection })
              }
              className={`w-11 h-6 flex items-center rounded-full p-1 transition duration-300 cursor-pointer ${
                settings.antiScreenshotProtection ? 'bg-indigo-600' : 'bg-slate-800'
              }`}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition duration-300 ${
                  settings.antiScreenshotProtection ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Auto-Lock Delay */}
          <div className="flex items-center justify-between py-2">
            <div>
              <div className="text-xs font-semibold text-slate-200">Auto-Lock Inactivity Timeout</div>
              <div className="text-[11px] text-slate-400">Locks the app automatically when idle</div>
            </div>
            <select
              value={settings.autoLockMinutes}
              onChange={(e) => onUpdateSettings({ ...settings, autoLockMinutes: Number(e.target.value) })}
              className="px-3 py-1.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value={0}>Immediately</option>
              <option value={5}>5 minutes</option>
              <option value={15}>15 minutes</option>
              <option value={30}>30 minutes</option>
              <option value={-1}>Never</option>
            </select>
          </div>
        </div>

        {/* Section 2: Biometrics & Fingerprint Backup (.dat) */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
            <Fingerprint className="w-4 h-4 text-emerald-400" />
            <h3 className="text-sm font-bold text-slate-200">Biometrics & Fingerprint Backup (.dat)</h3>
          </div>

          <p className="text-xs text-slate-400">
            Register your biometric credential to generate a cryptographically signed emergency unlock backup file (fingerprint_backup.dat).
          </p>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={handleRegisterFingerprint}
              id="btn_register_fingerprint"
              className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-600/20 transition active:scale-95"
            >
              <Fingerprint className="w-4 h-4" />
              <span>{settings.fingerprintRegistered ? 'Re-Register Biometrics' : 'Register Fingerprint'}</span>
            </button>

            <button
              onClick={handleExportFingerprintDat}
              id="btn_export_fingerprint_dat"
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
              title="Download fingerprint_backup.dat"
            >
              <Download className="w-4 h-4 text-emerald-400" />
              <span>Export Fingerprint Backup (.dat)</span>
            </button>

            <input
              type="file"
              ref={importFingerprintRef}
              onChange={handleImportFingerprintDat}
              accept=".dat"
              className="hidden"
              id="input_import_fingerprint_dat"
            />
            <button
              onClick={() => importFingerprintRef.current?.click()}
              id="btn_import_fingerprint_dat"
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
            >
              <Upload className="w-4 h-4 text-cyan-400" />
              <span>Import & Verify Backup (.dat)</span>
            </button>
          </div>
        </div>

        {/* Section 3: Visual & Gallery Customization */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
            <Sliders className="w-4 h-4 text-cyan-400" />
            <h3 className="text-sm font-bold text-slate-200">Catalog & Gallery Customization</h3>
          </div>

          {/* Grid Columns */}
          <div className="flex items-center justify-between py-2 border-b border-slate-800/60">
            <div>
              <div className="text-xs font-semibold text-slate-200">Gallery Grid Columns</div>
              <div className="text-[11px] text-slate-400">Controls thumbnail density across the catalog</div>
            </div>
            <div className="flex items-center gap-1 bg-slate-950 p-1 rounded-xl border border-slate-800">
              {[2, 3, 4, 5].map((cols) => (
                <button
                  key={cols}
                  onClick={() => onUpdateSettings({ ...settings, galleryGridColumns: cols })}
                  className={`px-3 py-1 rounded-lg text-xs font-mono font-bold transition ${
                    settings.galleryGridColumns === cols
                      ? 'bg-indigo-600 text-white'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {cols}
                </button>
              ))}
            </div>
          </div>

          {/* Image Fit Mode */}
          <div className="flex items-center justify-between py-2 border-b border-slate-800/60">
            <div>
              <div className="text-xs font-semibold text-slate-200">Image Aspect Ratio Mode</div>
              <div className="text-[11px] text-slate-400">Fit entire photo vs. crop square fill</div>
            </div>
            <div className="flex items-center gap-1 bg-slate-950 p-1 rounded-xl border border-slate-800">
              <button
                onClick={() => onUpdateSettings({ ...settings, imageFitMode: 'fit' })}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition ${
                  settings.imageFitMode === 'fit'
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Fit (Uncut)
              </button>
              <button
                onClick={() => onUpdateSettings({ ...settings, imageFitMode: 'crop' })}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition ${
                  settings.imageFitMode === 'crop'
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Crop (Fill)
              </button>
            </div>
          </div>

          {/* High Contrast Borders */}
          <div className="flex items-center justify-between py-2 border-b border-slate-800/60">
            <div>
              <div className="text-xs font-semibold text-slate-200">High Contrast Card Borders</div>
              <div className="text-[11px] text-slate-400">Adds prominent edges around image cards for clarity</div>
            </div>
            <button
              onClick={() =>
                onUpdateSettings({ ...settings, highContrastBorders: !settings.highContrastBorders })
              }
              className={`w-11 h-6 flex items-center rounded-full p-1 transition duration-300 cursor-pointer ${
                settings.highContrastBorders ? 'bg-indigo-600' : 'bg-slate-800'
              }`}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition duration-300 ${
                  settings.highContrastBorders ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Slideshow Speed */}
          <div className="flex items-center justify-between py-2">
            <div>
              <div className="text-xs font-semibold text-slate-200">Slideshow Transition Speed</div>
              <div className="text-[11px] text-slate-400">Duration per photo during playback</div>
            </div>
            <select
              value={settings.slideshowSpeedMs}
              onChange={(e) => onUpdateSettings({ ...settings, slideshowSpeedMs: Number(e.target.value) })}
              className="px-3 py-1.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value={800}>0.8s (Fast)</option>
              <option value={1500}>1.5s (Standard)</option>
              <option value={2500}>2.5s (Relaxed)</option>
              <option value={4000}>4.0s (Slow)</option>
            </select>
          </div>
        </div>

        {/* Section 4: App Tools & System Actions */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 flex flex-col gap-4">
          <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
            <Smartphone className="w-4 h-4 text-purple-400" />
            <h3 className="text-sm font-bold text-slate-200">App Branding & Documentation</h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <button
              onClick={onOpenManual}
              id="btn_settings_open_manual"
              className="flex items-center gap-2.5 p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold transition"
            >
              <BookOpen className="w-4 h-4 text-indigo-400" />
              <span>User Manual</span>
            </button>

            <button
              onClick={onOpenShareQr}
              id="btn_settings_share_qr"
              className="flex items-center gap-2.5 p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold transition"
            >
              <Share2 className="w-4 h-4 text-cyan-400" />
              <span>Share App QR</span>
            </button>

            <button
              onClick={onOpenLogoPicker}
              id="btn_settings_change_logo"
              className="flex items-center gap-2.5 p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold transition"
            >
              <ImageIcon className="w-4 h-4 text-purple-400" />
              <span>Custom App Logo</span>
            </button>

            <button
              onClick={onOpenProfile}
              id="btn_settings_user_profile"
              className="flex items-center gap-2.5 p-3 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold transition"
            >
              <FileCheck2 className="w-4 h-4 text-amber-400" />
              <span>User Profile & ID</span>
            </button>
          </div>

          <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between">
            <span className="text-xs text-slate-400">Clear local cache & reset sample data</span>
            <button
              onClick={onClearCache}
              id="btn_settings_clear_cache"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-red-500/10 border border-red-500/30 text-red-400 hover:bg-red-500/20 transition"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Clear Cache</span>
            </button>
          </div>
        </div>

        {/* Change PIN Modal */}
        {pinChangeModal && (
          <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <KeyRound className="w-5 h-5 text-indigo-400" />
                <h3 className="text-base font-bold text-slate-100">Configure 6-Digit PIN</h3>
              </div>
              <p className="text-xs text-slate-400">
                Enter your new 6-digit numeric security PIN for locking and unlocking the app.
              </p>

              <form onSubmit={handleSaveNewPin} className="flex flex-col gap-3 text-xs">
                <div>
                  <label className="block text-slate-300 mb-1">New 6-Digit PIN</label>
                  <input
                    type="password"
                    maxLength={6}
                    value={newPinInput}
                    onChange={(e) => setNewPinInput(e.target.value)}
                    placeholder="••••••"
                    required
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-center text-base font-mono tracking-widest text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>

                <div>
                  <label className="block text-slate-300 mb-1">Confirm 6-Digit PIN</label>
                  <input
                    type="password"
                    maxLength={6}
                    value={confirmPinInput}
                    onChange={(e) => setConfirmPinInput(e.target.value)}
                    placeholder="••••••"
                    required
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-center text-base font-mono tracking-widest text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>

                {pinError && <p className="text-xs text-red-400 text-center font-medium">{pinError}</p>}
                {pinSuccess && <p className="text-xs text-emerald-400 text-center font-semibold">PIN saved successfully!</p>}

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setPinChangeModal(false)}
                    className="px-3.5 py-2 rounded-xl text-slate-400 hover:text-white"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold"
                  >
                    Set PIN
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
