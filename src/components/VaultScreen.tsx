import React, { useState, useRef } from 'react';
import { VaultContainerInfo, VaultImage } from '../types';
import { encryptVaultContainer, decryptVaultContainer, generateRandomSalt } from '../utils/crypto';
import { VaultModalViewer } from './VaultModalViewer';
import JSZip from 'jszip';
import {
  ShieldAlert,
  Lock,
  Unlock,
  Plus,
  Trash2,
  Download,
  Upload,
  FolderOpen,
  Image as ImageIcon,
  KeyRound,
  HardDrive,
  FileCheck,
  Sparkles,
  Archive
} from 'lucide-react';

interface VaultScreenProps {
  containers: VaultContainerInfo[];
  onUpdateContainers: (containers: VaultContainerInfo[]) => void;
}

export const VaultScreen: React.FC<VaultScreenProps> = ({
  containers,
  onUpdateContainers,
}) => {
  const [activeContainerId, setActiveContainerId] = useState<string | null>(
    containers.length > 0 ? containers[0].id : null
  );
  const [pinInput, setPinInput] = useState('');
  const [unlockError, setUnlockError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newContainerName, setNewContainerName] = useState('secure_photos.dat');
  const [newContainerSizeMb, setNewContainerSizeMb] = useState(50);
  const [newContainerPin, setNewContainerPin] = useState('');
  const [newContainerPadding, setNewContainerPadding] = useState(true);
  const [activeViewerIndex, setActiveViewerIndex] = useState<number | null>(null);

  const importDatRef = useRef<HTMLInputElement>(null);
  const addImagesRef = useRef<HTMLInputElement>(null);

  const activeContainer = containers.find((c) => c.id === activeContainerId) || containers[0] || null;

  // Create new container
  const handleCreateContainer = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newContainerPin || newContainerPin.length < 4) {
      alert('Please enter a PIN with at least 4 digits/characters.');
      return;
    }

    const salt = generateRandomSalt(16);
    const fileName = newContainerName.endsWith('.dat') ? newContainerName : `${newContainerName}.dat`;
    const targetSizeBytes = newContainerSizeMb * 1024 * 1024;

    const initialEnvelope = encryptVaultContainer(
      JSON.stringify({ images: [], paddingBytes: newContainerPadding ? 1024 : 0 }),
      newContainerPin,
      salt
    );

    const newContainer: VaultContainerInfo = {
      id: 'vault_' + Date.now(),
      fileName,
      sizeBytes: 1024,
      targetSizeBytes,
      allocatedMb: newContainerSizeMb,
      isUnlocked: true,
      images: [],
      encryptedData: initialEnvelope,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    const updated = [newContainer, ...containers];
    onUpdateContainers(updated);
    setActiveContainerId(newContainer.id);
    setShowCreateModal(false);
    setNewContainerPin('');
  };

  // Unlock container
  const handleUnlock = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeContainer || !activeContainer.encryptedData) return;

    const decryptedJson = decryptVaultContainer(activeContainer.encryptedData, pinInput);
    if (!decryptedJson) {
      setUnlockError('Incorrect PIN or corrupted container payload.');
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
      setPinInput('');
      setUnlockError(null);
    } catch {
      setUnlockError('Container data decoding failed.');
    }
  };

  // Lock container
  const handleLockContainer = () => {
    if (!activeContainer) return;
    const pin = prompt('Enter a PIN to lock and securely re-encrypt this container:', '123456');
    if (!pin) return;

    const salt = generateRandomSalt(16);
    const payloadJson = JSON.stringify({ images: activeContainer.images });
    const encryptedData = encryptVaultContainer(payloadJson, pin, salt);

    const updated = containers.map((c) =>
      c.id === activeContainer.id
        ? { ...c, isUnlocked: false, images: [], encryptedData }
        : c
    );
    onUpdateContainers(updated);
  };

  // Import images into container
  const handleAddImages = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || !activeContainer) return;

    const files = Array.from(e.target.files);
    const newImages: VaultImage[] = [];

    for (const file of files) {
      const dataUrl = await new Promise<string>((resolve) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.readAsDataURL(file);
      });

      newImages.push({
        id: 'img_' + Math.random().toString(36).substring(2, 9),
        name: file.name,
        dataUrl,
        sizeBytes: file.size,
        addedAt: Date.now(),
      });
    }

    const mergedImages = [...activeContainer.images, ...newImages];
    const updated = containers.map((c) =>
      c.id === activeContainer.id
        ? {
            ...c,
            images: mergedImages,
            sizeBytes: mergedImages.reduce((sum, img) => sum + img.sizeBytes, 0),
            updatedAt: Date.now(),
          }
        : c
    );
    onUpdateContainers(updated);
    e.target.value = '';
  };

  // Delete image
  const handleDeleteImage = (imgId: string) => {
    if (!activeContainer) return;
    const remaining = activeContainer.images.filter((img) => img.id !== imgId);
    const updated = containers.map((c) =>
      c.id === activeContainer.id
        ? {
            ...c,
            images: remaining,
            sizeBytes: remaining.reduce((sum, img) => sum + img.sizeBytes, 0),
          }
        : c
    );
    onUpdateContainers(updated);
  };

  // Export .dat container
  const handleExportDat = () => {
    if (!activeContainer) return;
    const pin = prompt('Enter PIN to encrypt the exported container file:', '123456');
    if (!pin) return;

    const salt = generateRandomSalt(16);
    const payload = JSON.stringify({
      version: '3.0',
      magic: 'VLT3',
      fileName: activeContainer.fileName,
      targetAllocatedMb: activeContainer.allocatedMb,
      timestamp: Date.now(),
      images: activeContainer.images,
    });

    const encryptedString = encryptVaultContainer(payload, pin, salt);
    const blob = new Blob([encryptedString], { type: 'application/octet-stream' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = activeContainer.fileName;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Import .dat file
  const handleImportDatFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || !e.target.files[0]) return;
    const file = e.target.files[0];
    const reader = new FileReader();

    reader.onload = () => {
      const content = reader.result as string;
      const newContainer: VaultContainerInfo = {
        id: 'vault_' + Date.now(),
        fileName: file.name,
        sizeBytes: file.size,
        targetSizeBytes: Math.max(file.size, 50 * 1024 * 1024),
        allocatedMb: Math.ceil(file.size / (1024 * 1024)) || 50,
        isUnlocked: false,
        images: [],
        encryptedData: content,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };
      const updated = [newContainer, ...containers];
      onUpdateContainers(updated);
      setActiveContainerId(newContainer.id);
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Extract all images to ZIP
  const handleExtractZip = async () => {
    if (!activeContainer || activeContainer.images.length === 0) return;
    const zip = new JSZip();

    activeContainer.images.forEach((img) => {
      const base64Data = img.dataUrl.split(',')[1];
      if (base64Data) {
        zip.file(img.name, base64Data, { base64: true });
      }
    });

    const content = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(content);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${activeContainer.fileName.replace('.dat', '')}_extracted.zip`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex-1 flex flex-col bg-slate-950 p-4 md:p-6 overflow-y-auto">
      <div className="max-w-7xl mx-auto w-full flex flex-col gap-6">
        {/* Header and Container Switcher */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
          <div>
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-purple-400" />
              <h2 className="text-lg font-bold text-slate-100">Encrypted DAT Image Vault</h2>
              <span className="text-[11px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-mono">
                AES-256 PBKDF2
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">
              Secure binary containers (.dat) with fixed-size allocation and dummy-padding for plausible deniability.
            </p>
          </div>

          {/* Container Actions */}
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setShowCreateModal(true)}
              id="btn_create_new_vault"
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white shadow-lg shadow-purple-600/20 transition active:scale-95"
            >
              <Plus className="w-4 h-4" />
              <span>New Container</span>
            </button>

            <input
              type="file"
              ref={importDatRef}
              onChange={handleImportDatFile}
              accept=".dat"
              className="hidden"
              id="input_import_dat"
            />
            <button
              onClick={() => importDatRef.current?.click()}
              id="btn_import_dat"
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
              title="Import .dat container file from storage"
            >
              <Upload className="w-4 h-4 text-purple-400" />
              <span>Import .DAT</span>
            </button>
          </div>
        </div>

        {/* Containers List Pills */}
        {containers.length > 0 && (
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {containers.map((container) => (
              <button
                key={container.id}
                onClick={() => {
                  setActiveContainerId(container.id);
                  setUnlockError(null);
                }}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold border transition whitespace-nowrap ${
                  activeContainer?.id === container.id
                    ? 'bg-purple-600/20 border-purple-500 text-purple-300 shadow-md'
                    : 'bg-slate-900 border-slate-800 text-slate-400 hover:bg-slate-850 hover:text-slate-200'
                }`}
              >
                {container.isUnlocked ? (
                  <Unlock className="w-3.5 h-3.5 text-emerald-400" />
                ) : (
                  <Lock className="w-3.5 h-3.5 text-amber-400" />
                )}
                <span>{container.fileName}</span>
                <span className="font-mono text-[10px] text-slate-500">
                  ({container.allocatedMb} MB)
                </span>
              </button>
            ))}
          </div>
        )}

        {/* Active Container Workspace */}
        {!activeContainer ? (
          <div className="flex-1 flex flex-col items-center justify-center p-12 text-center bg-slate-900/40 rounded-3xl border border-slate-800/80">
            <Archive className="w-16 h-16 text-slate-600 mb-3" />
            <h3 className="text-base font-bold text-slate-300">No Containers Created Yet</h3>
            <p className="text-xs text-slate-400 max-w-sm mb-4">
              Create a new encrypted DAT container to store confidential photos, or import an existing container.
            </p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-purple-600 hover:bg-purple-500 text-white"
            >
              Create First Container
            </button>
          </div>
        ) : !activeContainer.isUnlocked ? (
          /* Locked State: PIN entry */
          <div className="flex-1 flex flex-col items-center justify-center p-8 bg-slate-900/60 rounded-3xl border border-slate-800 min-h-[380px]">
            <div className="w-14 h-14 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 mb-4 shadow-inner">
              <Lock className="w-7 h-7" />
            </div>
            <h3 className="text-base font-bold text-slate-100 mb-1">{activeContainer.fileName} is Locked</h3>
            <p className="text-xs text-slate-400 mb-6 max-w-xs text-center">
              Enter the 6-digit or custom security PIN to decrypt and access images.
            </p>

            <form onSubmit={handleUnlock} className="w-full max-w-xs flex flex-col gap-3">
              <div className="relative">
                <input
                  type="password"
                  value={pinInput}
                  onChange={(e) => setPinInput(e.target.value)}
                  placeholder="Enter Vault PIN…"
                  id="input_vault_pin"
                  autoFocus
                  className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-center text-sm font-mono tracking-widest text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>

              {unlockError && (
                <p className="text-xs text-red-400 font-medium text-center">{unlockError}</p>
              )}

              <button
                type="submit"
                id="btn_unlock_vault"
                className="w-full py-2.5 rounded-xl font-semibold text-xs bg-purple-600 hover:bg-purple-500 text-white shadow-lg shadow-purple-600/20 transition active:scale-95 flex items-center justify-center gap-2"
              >
                <KeyRound className="w-4 h-4" />
                <span>Unlock Container</span>
              </button>
            </form>
          </div>
        ) : (
          /* Unlocked State: Photo Gallery and Controls */
          <div className="flex flex-col gap-4">
            {/* Action Bar */}
            <div className="flex flex-wrap items-center justify-between gap-3 bg-slate-900/50 border border-slate-800/80 p-3.5 rounded-2xl">
              <div className="flex items-center gap-3">
                <span className="text-xs font-semibold text-slate-300">
                  {activeContainer.images.length} photo(s) in container
                </span>
                <span className="text-[11px] font-mono text-slate-500">
                  Allocated: {activeContainer.allocatedMb} MB
                </span>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <input
                  type="file"
                  ref={addImagesRef}
                  onChange={handleAddImages}
                  multiple
                  accept="image/*"
                  className="hidden"
                  id="input_add_vault_images"
                />
                <button
                  onClick={() => addImagesRef.current?.click()}
                  id="btn_add_vault_images"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-purple-600 hover:bg-purple-500 text-white shadow-sm transition active:scale-95"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Images</span>
                </button>

                <button
                  onClick={handleExtractZip}
                  id="btn_extract_vault_zip"
                  disabled={activeContainer.images.length === 0}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 disabled:opacity-50 transition"
                  title="Export all pictures as a ZIP"
                >
                  <Download className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Extract ZIP</span>
                </button>

                <button
                  onClick={handleExportDat}
                  id="btn_export_vault_dat"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
                  title="Download encrypted .dat container file"
                >
                  <FileCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Export .DAT</span>
                </button>

                <button
                  onClick={handleLockContainer}
                  id="btn_lock_vault"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-amber-500/10 border border-amber-500/30 text-amber-300 hover:bg-amber-500/20 transition active:scale-95"
                >
                  <Lock className="w-3.5 h-3.5" />
                  <span>Lock Vault</span>
                </button>
              </div>
            </div>

            {/* Images Grid */}
            {activeContainer.images.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-12 bg-slate-900/30 rounded-2xl border border-slate-850 text-center">
                <ImageIcon className="w-12 h-12 text-slate-600 mb-2" />
                <h4 className="text-sm font-semibold text-slate-300">Container is empty</h4>
                <p className="text-xs text-slate-400 mb-4">Click "Add Images" above to add photos to this encrypted vault.</p>
                <button
                  onClick={() => addImagesRef.current?.click()}
                  className="px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-purple-600 text-white"
                >
                  Import Photos
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5">
                {activeContainer.images.map((img, idx) => (
                  <div
                    key={img.id}
                    className="group relative bg-slate-900 rounded-xl overflow-hidden border border-slate-800 hover:border-purple-500/50 transition-all shadow-sm"
                  >
                    <div
                      onClick={() => setActiveViewerIndex(idx)}
                      className="aspect-square bg-slate-950 flex items-center justify-center cursor-pointer overflow-hidden"
                    >
                      <img
                        src={img.dataUrl}
                        alt={img.name}
                        className="w-full h-full object-cover transition group-hover:scale-105"
                      />
                    </div>

                    <div className="p-2.5 flex items-center justify-between text-xs bg-slate-900/90 border-t border-slate-800">
                      <span className="truncate max-w-[110px] text-slate-200 text-[11px] font-medium" title={img.name}>
                        {img.name}
                      </span>
                      <button
                        onClick={() => handleDeleteImage(img.id)}
                        className="p-1 rounded text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition"
                        title="Delete image from container"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Modal Viewer */}
        {activeViewerIndex !== null && activeContainer && (
          <VaultModalViewer
            images={activeContainer.images}
            currentIndex={activeViewerIndex}
            onClose={() => setActiveViewerIndex(null)}
            onNext={() => setActiveViewerIndex((prev) => (prev! + 1) % activeContainer.images.length)}
            onPrevious={() =>
              setActiveViewerIndex((prev) => (prev! - 1 + activeContainer.images.length) % activeContainer.images.length)
            }
          />
        )}

        {/* Create Container Dialog */}
        {showCreateModal && (
          <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <HardDrive className="w-5 h-5 text-purple-400" />
                <h3 className="text-base font-bold text-slate-100">Create Encrypted DAT Container</h3>
              </div>
              <p className="text-xs text-slate-400">
                Configure container name, dummy allocation size, and a protective security PIN.
              </p>

              <form onSubmit={handleCreateContainer} className="flex flex-col gap-3.5 text-xs">
                <div>
                  <label className="block text-slate-300 mb-1 font-medium">Container Filename</label>
                  <input
                    type="text"
                    value={newContainerName}
                    onChange={(e) => setNewContainerName(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-2 focus:ring-purple-500"
                  />
                </div>

                <div>
                  <label className="block text-slate-300 mb-1 font-medium">Target Allocated Size (MB)</label>
                  <select
                    value={newContainerSizeMb}
                    onChange={(e) => setNewContainerSizeMb(Number(e.target.value))}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-2 focus:ring-purple-500"
                  >
                    <option value={10}>10 MB (Quick preview)</option>
                    <option value={50}>50 MB (Recommended)</option>
                    <option value={100}>100 MB (Standard album)</option>
                    <option value={500}>500 MB (Large catalog)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-300 mb-1 font-medium">Access PIN (Min 4 digits)</label>
                  <input
                    type="password"
                    value={newContainerPin}
                    onChange={(e) => setNewContainerPin(e.target.value)}
                    placeholder="Enter security PIN"
                    required
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 font-mono tracking-widest focus:outline-none focus:ring-2 focus:ring-purple-500"
                  />
                </div>

                <div className="flex items-center gap-2 pt-1">
                  <input
                    type="checkbox"
                    id="chk_padding"
                    checked={newContainerPadding}
                    onChange={(e) => setNewContainerPadding(e.target.checked)}
                    className="rounded border-slate-700 text-purple-600 focus:ring-purple-500"
                  />
                  <label htmlFor="chk_padding" className="text-slate-300 cursor-pointer">
                    Enable uniform dummy-padding bytes for plausible deniability
                  </label>
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 font-medium"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-semibold shadow-md"
                  >
                    Create Container
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
