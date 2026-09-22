import React, { useState, useRef } from 'react';
import { SShowContainer, SShowImage } from '../types';
import { encryptSShowPayload, decryptSShowPayload, generateRandomPin } from '../utils/crypto';
import {
  Sparkles,
  Lock,
  Unlock,
  Plus,
  Play,
  Pause,
  Key,
  Download,
  Upload,
  Copy,
  Check,
  FastForward,
  ChevronLeft,
  ChevronRight,
  Maximize2,
  X,
  FileCheck2,
  Share2
} from 'lucide-react';

interface SShowScreenProps {
  containers: SShowContainer[];
  onUpdateContainers: (containers: SShowContainer[]) => void;
}

export const SShowScreen: React.FC<SShowScreenProps> = ({
  containers,
  onUpdateContainers,
}) => {
  const [activeContainerId, setActiveContainerId] = useState<string | null>(
    containers.length > 0 ? containers[0].id : null
  );
  const [showEncryptModal, setShowEncryptModal] = useState(false);
  const [encryptFileName, setEncryptFileName] = useState('confidential_presentation.secure');
  const [encryptPassword, setEncryptPassword] = useState('');
  const [pendingImages, setPendingImages] = useState<SShowImage[]>([]);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Decrypt dialog
  const [decryptPasswordInput, setDecryptPasswordInput] = useState('');
  const [decryptError, setDecryptError] = useState<string | null>(null);

  // Slideshow state
  const [isSlideshowActive, setIsSlideshowActive] = useState(false);
  const [isFastForward, setIsFastForward] = useState(false);
  const [currentSlideIndex, setCurrentSlideIndex] = useState(0);
  const [fullscreenImageIndex, setFullscreenImageIndex] = useState<number | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const importSecureRef = useRef<HTMLInputElement>(null);

  const activeContainer = containers.find((c) => c.id === activeContainerId) || containers[0] || null;

  // Prepare images for encryption
  const handleSelectImagesToEncrypt = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const files = Array.from(e.target.files);
    const readImages: SShowImage[] = [];

    for (const file of files) {
      const dataUrl = await new Promise<string>((resolve) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.readAsDataURL(file);
      });

      readImages.push({
        id: 'ss_' + Math.random().toString(36).substring(2, 8),
        name: file.name,
        dataUrl,
        sizeBytes: file.size,
        addedAt: Date.now(),
      });
    }

    setPendingImages(readImages);
    setEncryptPassword(generateRandomPin());
    setShowEncryptModal(true);
    e.target.value = '';
  };

  // Perform Encryption
  const handleConfirmEncrypt = (e: React.FormEvent) => {
    e.preventDefault();
    if (pendingImages.length === 0) return;

    const fileName = encryptFileName.endsWith('.secure') ? encryptFileName : `${encryptFileName}.secure`;
    const payload = JSON.stringify({
      version: '1.0',
      type: 'SSHOW_SECURE_PAYLOAD',
      fileName,
      createdAt: Date.now(),
      images: pendingImages,
    });

    const encryptedBase64 = encryptSShowPayload(payload, encryptPassword);

    const newContainer: SShowContainer = {
      id: 'sshow_' + Date.now(),
      fileName,
      salt: 'Salted__',
      encryptedBlobBase64: encryptedBase64,
      images: pendingImages,
      passwordHint: encryptPassword,
      isUnlocked: true,
      createdAt: Date.now(),
    };

    const updated = [newContainer, ...containers];
    onUpdateContainers(updated);
    setActiveContainerId(newContainer.id);
    setShowEncryptModal(false);
    setPendingImages([]);
  };

  // Decrypt Container
  const handleDecrypt = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeContainer || !activeContainer.encryptedBlobBase64) return;

    const decryptedJson = decryptSShowPayload(activeContainer.encryptedBlobBase64, decryptPasswordInput);
    if (!decryptedJson) {
      setDecryptError('Invalid Password / PIN. Decryption failed.');
      return;
    }

    try {
      const payload = JSON.parse(decryptedJson);
      const updated = containers.map((c) =>
        c.id === activeContainer.id
          ? { ...c, isUnlocked: true, images: payload.images || [] }
          : c
      );
      onUpdateContainers(updated);
      setDecryptPasswordInput('');
      setDecryptError(null);
    } catch {
      setDecryptError('Decryption succeeded but data payload format was corrupt.');
    }
  };

  // Lock SShow
  const handleLockSShow = () => {
    if (!activeContainer) return;
    const pwd = prompt('Enter a password to lock and encrypt SShow:', activeContainer.passwordHint || '123456');
    if (!pwd) return;

    const payload = JSON.stringify({
      version: '1.0',
      type: 'SSHOW_SECURE_PAYLOAD',
      fileName: activeContainer.fileName,
      createdAt: activeContainer.createdAt,
      images: activeContainer.images,
    });
    const encryptedBase64 = encryptSShowPayload(payload, pwd);

    const updated = containers.map((c) =>
      c.id === activeContainer.id
        ? { ...c, isUnlocked: false, images: [], encryptedBlobBase64: encryptedBase64, passwordHint: pwd }
        : c
    );
    onUpdateContainers(updated);
    setIsSlideshowActive(false);
  };

  // Export .secure file
  const handleExportSecure = () => {
    if (!activeContainer || !activeContainer.encryptedBlobBase64) return;
    const blob = new Blob([activeContainer.encryptedBlobBase64], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = activeContainer.fileName;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Import .secure file
  const handleImportSecure = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || !e.target.files[0]) return;
    const file = e.target.files[0];
    const reader = new FileReader();

    reader.onload = () => {
      const content = reader.result as string;
      const newContainer: SShowContainer = {
        id: 'sshow_' + Date.now(),
        fileName: file.name,
        salt: 'Salted__',
        encryptedBlobBase64: content,
        images: [],
        isUnlocked: false,
        createdAt: Date.now(),
      };
      const updated = [newContainer, ...containers];
      onUpdateContainers(updated);
      setActiveContainerId(newContainer.id);
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Slideshow interval
  React.useEffect(() => {
    if (!isSlideshowActive || !activeContainer || activeContainer.images.length === 0) return;
    const intervalTime = isFastForward ? 350 : 1500;
    const timer = setInterval(() => {
      setCurrentSlideIndex((prev) => (prev + 1) % activeContainer.images.length);
    }, intervalTime);

    return () => clearInterval(timer);
  }, [isSlideshowActive, isFastForward, activeContainer]);

  const copyPasswordToClipboard = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2500);
  };

  return (
    <div className="flex-1 flex flex-col bg-slate-950 p-4 md:p-6 overflow-y-auto">
      <div className="max-w-7xl mx-auto w-full flex flex-col gap-6">
        {/* Banner */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
          <div>
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-rose-400" />
              <h2 className="text-lg font-bold text-slate-100">🛡️ SShow (Secure Show)</h2>
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 font-mono">
                OpenSSL AES-256-CBC
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Client-side encrypted presentations and confidential image showcases with instant slideshow and sharing.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleSelectImagesToEncrypt}
              multiple
              accept="image/*"
              className="hidden"
              id="input_select_sshow_images"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              id="btn_encrypt_new_sshow"
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-gradient-to-r from-rose-500 to-amber-500 hover:from-rose-400 hover:to-amber-400 text-white shadow-lg shadow-rose-500/20 transition active:scale-95"
            >
              <Plus className="w-4 h-4" />
              <span>Encrypt Images (.secure)</span>
            </button>

            <input
              type="file"
              ref={importSecureRef}
              onChange={handleImportSecure}
              accept=".secure"
              className="hidden"
              id="input_import_secure"
            />
            <button
              onClick={() => importSecureRef.current?.click()}
              id="btn_import_secure"
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
              title="Import .secure file"
            >
              <Upload className="w-4 h-4 text-amber-400" />
              <span>Import .secure</span>
            </button>
          </div>
        </div>

        {/* Containers switcher */}
        {containers.length > 0 && (
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {containers.map((container) => (
              <button
                key={container.id}
                onClick={() => {
                  setActiveContainerId(container.id);
                  setDecryptError(null);
                  setCurrentSlideIndex(0);
                }}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold border transition whitespace-nowrap ${
                  activeContainer?.id === container.id
                    ? 'bg-rose-500/20 border-rose-500 text-rose-300 shadow-md'
                    : 'bg-slate-900 border-slate-800 text-slate-400 hover:bg-slate-850 hover:text-slate-200'
                }`}
              >
                {container.isUnlocked ? (
                  <Unlock className="w-3.5 h-3.5 text-emerald-400" />
                ) : (
                  <Lock className="w-3.5 h-3.5 text-amber-400" />
                )}
                <span>{container.fileName}</span>
                {container.passwordHint && (
                  <span className="font-mono text-[10px] text-slate-500">
                    PIN: {container.passwordHint}
                  </span>
                )}
              </button>
            ))}
          </div>
        )}

        {/* Active Container */}
        {!activeContainer ? (
          <div className="flex-1 flex flex-col items-center justify-center p-12 text-center bg-slate-900/40 rounded-3xl border border-slate-800/80">
            <Sparkles className="w-16 h-16 text-slate-600 mb-3" />
            <h3 className="text-base font-bold text-slate-300">No SShow Containers</h3>
            <p className="text-xs text-slate-400 max-w-sm mb-4">
              Select images from your device to encrypt them into a portable .secure presentation container.
            </p>
            <button
              onClick={() => fileInputRef.current?.click()}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-500 text-white"
            >
              Select Images to Encrypt
            </button>
          </div>
        ) : !activeContainer.isUnlocked ? (
          /* Locked SShow */
          <div className="flex-1 flex flex-col items-center justify-center p-8 bg-slate-900/60 rounded-3xl border border-slate-800 min-h-[380px]">
            <div className="w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 mb-4">
              <Lock className="w-7 h-7" />
            </div>
            <h3 className="text-base font-bold text-slate-100 mb-1">{activeContainer.fileName} is Encrypted</h3>
            <p className="text-xs text-slate-400 mb-6 max-w-xs text-center">
              Enter the OpenSSL password or 6-digit PIN used to encrypt this presentation.
            </p>

            <form onSubmit={handleDecrypt} className="w-full max-w-xs flex flex-col gap-3">
              <input
                type="password"
                value={decryptPasswordInput}
                onChange={(e) => setDecryptPasswordInput(e.target.value)}
                placeholder="Enter password / PIN…"
                id="input_sshow_password"
                autoFocus
                className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-center text-sm font-mono tracking-widest text-slate-100 focus:outline-none focus:ring-2 focus:ring-rose-500"
              />

              {decryptError && (
                <p className="text-xs text-red-400 font-medium text-center">{decryptError}</p>
              )}

              <button
                type="submit"
                id="btn_decrypt_sshow"
                className="w-full py-2.5 rounded-xl font-semibold text-xs bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-600/20 transition active:scale-95 flex items-center justify-center gap-2"
              >
                <Key className="w-4 h-4" />
                <span>Decrypt & View Presentation</span>
              </button>
            </form>
          </div>
        ) : (
          /* Unlocked SShow */
          <div className="flex flex-col gap-4">
            {/* Toolbar */}
            <div className="flex flex-wrap items-center justify-between gap-3 bg-slate-900/50 border border-slate-800/80 p-3.5 rounded-2xl">
              <div className="flex items-center gap-3">
                <span className="text-xs font-semibold text-slate-300">
                  {activeContainer.images.length} Slide(s)
                </span>
                {activeContainer.passwordHint && (
                  <button
                    onClick={() => copyPasswordToClipboard(activeContainer.passwordHint!)}
                    className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-slate-800 hover:bg-slate-700 text-xs font-mono text-slate-300 border border-slate-700 transition"
                    title="Click to copy security PIN"
                  >
                    <span>PIN: {activeContainer.passwordHint}</span>
                    {copiedCode === activeContainer.passwordHint ? (
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-slate-400" />
                    )}
                  </button>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <button
                  onClick={() => setIsSlideshowActive(!isSlideshowActive)}
                  id="btn_sshow_toggle_play"
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold shadow-sm transition active:scale-95 ${
                    isSlideshowActive
                      ? 'bg-amber-600 text-white'
                      : 'bg-rose-600 hover:bg-rose-500 text-white'
                  }`}
                >
                  {isSlideshowActive ? (
                    <>
                      <Pause className="w-3.5 h-3.5" />
                      <span>Pause</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-3.5 h-3.5" />
                      <span>Play Slideshow</span>
                    </>
                  )}
                </button>

                {isSlideshowActive && (
                  <button
                    onClick={() => setIsFastForward(!isFastForward)}
                    className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition ${
                      isFastForward
                        ? 'bg-amber-500/20 border-amber-500/40 text-amber-300'
                        : 'bg-slate-800 border-slate-700 text-slate-300'
                    }`}
                  >
                    <FastForward className="w-3.5 h-3.5" />
                    <span>0.3s</span>
                  </button>
                )}

                <button
                  onClick={handleExportSecure}
                  id="btn_export_secure"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
                  title="Download .secure encrypted file"
                >
                  <Download className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Export .secure</span>
                </button>

                <button
                  onClick={handleLockSShow}
                  id="btn_lock_sshow"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-red-500/10 hover:text-red-400 text-slate-300 border border-slate-700 transition"
                >
                  <Lock className="w-3.5 h-3.5" />
                  <span>Lock</span>
                </button>
              </div>
            </div>

            {/* Slideshow Presentation Stage */}
            {activeContainer.images.length > 0 && (
              <div className="relative w-full aspect-[16/9] sm:aspect-[21/9] max-h-[460px] bg-slate-950 rounded-2xl overflow-hidden border border-slate-800 flex items-center justify-center shadow-2xl">
                <img
                  src={activeContainer.images[currentSlideIndex].dataUrl}
                  alt={activeContainer.images[currentSlideIndex].name}
                  className="w-full h-full object-contain p-4 transition-all duration-300"
                />

                {/* Left/Right Overlays */}
                <button
                  onClick={() =>
                    setCurrentSlideIndex(
                      (prev) => (prev - 1 + activeContainer.images.length) % activeContainer.images.length
                    )
                  }
                  className="absolute left-3 top-1/2 -translate-y-1/2 p-2.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 shadow-lg"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button
                  onClick={() =>
                    setCurrentSlideIndex((prev) => (prev + 1) % activeContainer.images.length)
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 p-2.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 shadow-lg"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>

                {/* Badge Overlay */}
                <div className="absolute top-3 left-3 flex items-center gap-2">
                  <span className="px-2.5 py-1 rounded-lg bg-slate-900/85 text-rose-300 border border-slate-700 font-mono text-xs shadow">
                    Slide {currentSlideIndex + 1} / {activeContainer.images.length}
                  </span>
                  <span className="px-2.5 py-1 rounded-lg bg-slate-900/85 text-slate-200 border border-slate-700 text-xs shadow truncate max-w-[200px]">
                    {activeContainer.images[currentSlideIndex].name}
                  </span>
                </div>

                <button
                  onClick={() => setFullscreenImageIndex(currentSlideIndex)}
                  className="absolute top-3 right-3 p-2 rounded-lg bg-slate-900/85 hover:bg-slate-800 text-slate-300 border border-slate-700"
                  title="Fullscreen"
                >
                  <Maximize2 className="w-4 h-4" />
                </button>
              </div>
            )}

            {/* Thumbnail Strip */}
            <div className="flex items-center gap-2.5 overflow-x-auto p-2 bg-slate-900/40 rounded-xl border border-slate-800">
              {activeContainer.images.map((img, idx) => (
                <button
                  key={img.id}
                  onClick={() => setCurrentSlideIndex(idx)}
                  className={`relative flex-shrink-0 w-16 h-16 rounded-lg overflow-hidden border-2 transition ${
                    currentSlideIndex === idx
                      ? 'border-rose-500 ring-2 ring-rose-500/30 scale-105'
                      : 'border-slate-800 opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img.dataUrl} alt={img.name} className="w-full h-full object-cover" />
                  <span className="absolute bottom-0 inset-x-0 bg-slate-950/80 text-[9px] font-mono text-center text-slate-300">
                    {idx + 1}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Encrypt Setup Dialog */}
        {showEncryptModal && (
          <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-rose-400" />
                <h3 className="text-base font-bold text-slate-100">Encrypt Selected Images</h3>
              </div>
              <p className="text-xs text-slate-400">
                Packaging {pendingImages.length} image(s) using OpenSSL AES-256-CBC compatible encryption.
              </p>

              <form onSubmit={handleConfirmEncrypt} className="flex flex-col gap-3.5 text-xs">
                <div>
                  <label className="block text-slate-300 mb-1 font-medium">Filename (.secure)</label>
                  <input
                    type="text"
                    value={encryptFileName}
                    onChange={(e) => setEncryptFileName(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-slate-300 font-medium">Encryption Password / PIN</label>
                    <button
                      type="button"
                      onClick={() => setEncryptPassword(generateRandomPin())}
                      className="text-[11px] text-rose-400 hover:underline"
                    >
                      Generate Random PIN
                    </button>
                  </div>
                  <input
                    type="text"
                    value={encryptPassword}
                    onChange={(e) => setEncryptPassword(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 font-mono tracking-widest focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setShowEncryptModal(false)}
                    className="px-4 py-2 rounded-xl text-slate-400 hover:text-white"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold shadow-md"
                  >
                    Encrypt & Save
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Fullscreen Viewer */}
        {fullscreenImageIndex !== null && activeContainer && (
          <div className="fixed inset-0 z-50 bg-slate-950/95 flex flex-col justify-between backdrop-blur-md">
            <div className="flex items-center justify-between p-4 bg-slate-900/80 border-b border-slate-800">
              <span className="text-xs font-mono text-rose-300">
                {fullscreenImageIndex + 1} / {activeContainer.images.length}
              </span>
              <button
                onClick={() => setFullscreenImageIndex(null)}
                className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="flex-1 flex items-center justify-center p-4">
              <img
                src={activeContainer.images[fullscreenImageIndex].dataUrl}
                alt={activeContainer.images[fullscreenImageIndex].name}
                className="max-w-[95vw] max-h-[80vh] object-contain rounded-xl shadow-2xl"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
