import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  ExcelRowItem,
  LoadedFolderImage,
  DisplayImageItem,
  MainAppTab,
  AppSettings,
  UserProfile,
  VaultContainerInfo,
  SShowContainer,
  DualVaultSession,
} from './types';
import {
  parseExcelArrayBuffer,
  generateSampleCatalog,
  generateSampleImages,
  matchImagesToExcelRows,
  extractUniqueNames,
  extractUniqueColours,
} from './utils/excelParser';
import {
  loadAppSettings,
  saveAppSettings,
  loadUserProfile,
  saveUserProfile,
  loadCachedExcelRows,
  saveCachedExcelRows,
  loadCachedFolderImages,
  saveCachedFolderImages,
  loadVaultContainers,
  saveVaultContainers,
  loadSShowContainers,
  saveSShowContainers,
  clearAllStorage,
} from './utils/storage';
import { loadSavedDualSession, saveDualSession } from './utils/dualVaultFirebase';

// Components
import { HeaderBar } from './components/HeaderBar';
import { TabNavigation } from './components/TabNavigation';
import { ControlsSection } from './components/ControlsSection';
import { ImageGallery } from './components/ImageGallery';
import { ModalImageViewer } from './components/ModalImageViewer';
import { VaultScreen } from './components/VaultScreen';
import { SShowScreen } from './components/SShowScreen';
import { DualVaultConnectDialog } from './components/DualVaultConnectDialog';
import { SettingsScreen } from './components/SettingsScreen';
import { AppLockScreen } from './components/AppLockScreen';
import { ProfileDialog } from './components/ProfileDialog';
import { UserManualDialog } from './components/UserManualDialog';
import { ShareAppQrDialog } from './components/ShareAppQrDialog';
import { AppLogoDialog } from './components/AppLogoDialog';

export const App: React.FC = () => {
  // Application Settings & Profile State
  const [settings, setSettings] = useState<AppSettings>(loadAppSettings);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(loadUserProfile);

  // Security Lock State
  const [isLocked, setIsLocked] = useState<boolean>(() => settings.isPinLockEnabled);
  const [isWindowBlurred, setIsWindowBlurred] = useState<boolean>(false);

  // Navigation Tab State
  const [currentTab, setCurrentTab] = useState<MainAppTab>('EXCEL_CATALOG');

  // Excel & Folder Images Data
  const [excelRows, setExcelRows] = useState<ExcelRowItem[]>([]);
  const [excelFileName, setExcelFileName] = useState<string | null>(null);
  const [folderImages, setFolderImages] = useState<LoadedFolderImage[]>([]);
  const [folderName, setFolderName] = useState<string | null>(null);

  // Filters State
  const [selectedName, setSelectedName] = useState<string>('ALL');
  const [selectedColour, setSelectedColour] = useState<string>('ALL');

  // Processing State
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [loadingMessage, setLoadingMessage] = useState<string>('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Modal Lightbox Viewer State
  const [modalViewerIndex, setModalViewerIndex] = useState<number | null>(null);
  const [isSlideshowActive, setIsSlideshowActive] = useState<boolean>(false);

  // Vault Containers State
  const [vaultContainers, setVaultContainers] = useState<VaultContainerInfo[]>(loadVaultContainers);

  // SShow Containers State
  const [sshowContainers, setSShowContainers] = useState<SShowContainer[]>(loadSShowContainers);

  // Dual Vault Remote Sync State
  const [dualSession, setDualSession] = useState<DualVaultSession | null>(loadSavedDualSession);

  // Dialog Visibility Flags
  const [isManualOpen, setIsManualOpen] = useState<boolean>(false);
  const [isProfileOpen, setIsProfileOpen] = useState<boolean>(false);
  const [isShareQrOpen, setIsShareQrOpen] = useState<boolean>(false);
  const [isLogoOpen, setIsLogoOpen] = useState<boolean>(false);

  // Load cached data on first render
  useEffect(() => {
    const cachedRows = loadCachedExcelRows();
    const cachedImages = loadCachedFolderImages();

    if (cachedRows.length > 0) {
      setExcelRows(cachedRows);
      setExcelFileName('cached_catalog.xlsx');
    }
    if (cachedImages.length > 0) {
      setFolderImages(cachedImages);
      setFolderName(`${cachedImages.length} Saved Images`);
    }

    // If both empty, auto-seed with sample catalog for pristine first-load experience
    if (cachedRows.length === 0 && cachedImages.length === 0) {
      handleLoadSampleData();
    }
  }, []);

  // Save changes to storage
  const handleUpdateSettings = (newSettings: AppSettings) => {
    setSettings(newSettings);
    saveAppSettings(newSettings);
  };

  const handleUpdateProfile = (profile: UserProfile) => {
    setUserProfile(profile);
    saveUserProfile(profile);
    showToast('Profile updated successfully!');
  };

  const handleUpdateVaultContainers = (containers: VaultContainerInfo[]) => {
    setVaultContainers(containers);
    saveVaultContainers(containers);
  };

  const handleUpdateSShowContainers = (containers: SShowContainer[]) => {
    setSShowContainers(containers);
    saveSShowContainers(containers);
  };

  const handleUpdateDualSession = (session: DualVaultSession | null) => {
    setDualSession(session);
    saveDualSession(session);
  };

  // Toast notification helper
  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  // Anti-Screenshot & Screen Privacy: blur masking on window focus change
  useEffect(() => {
    if (!settings.antiScreenshotProtection) {
      setIsWindowBlurred(false);
      return;
    }

    const handleBlur = () => setIsWindowBlurred(true);
    const handleFocus = () => setIsWindowBlurred(false);

    window.addEventListener('blur', handleBlur);
    window.addEventListener('focus', handleFocus);
    return () => {
      window.removeEventListener('blur', handleBlur);
      window.removeEventListener('focus', handleFocus);
    };
  }, [settings.antiScreenshotProtection]);

  // Handle Excel File Selection
  const handleSelectExcelFile = async (file: File) => {
    setIsLoading(true);
    setLoadingMessage('Reading and parsing Excel spreadsheet…');
    try {
      const buffer = await file.arrayBuffer();
      const rows = parseExcelArrayBuffer(buffer);
      setExcelRows(rows);
      setExcelFileName(file.name);
      saveCachedExcelRows(rows);
      showToast(`Loaded ${rows.length} catalog rows from ${file.name}`);
    } catch (err) {
      alert('Error parsing Excel file. Ensure valid columns: CODE, NAME, TOP COLOR');
    } finally {
      setIsLoading(false);
      setLoadingMessage('');
    }
  };

  // Handle Image Folder / Multiple Files Selection
  const handleSelectImageFiles = async (files: FileList) => {
    setIsLoading(true);
    setLoadingMessage(`Importing ${files.length} image files…`);
    try {
      const loaded: LoadedFolderImage[] = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const dataUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader();
          reader.onload = () => resolve(reader.result as string);
          reader.readAsDataURL(file);
        });
        loaded.push({
          fileName: file.name,
          dataUrl,
        });
      }

      const merged = [...folderImages, ...loaded];
      setFolderImages(merged);
      setFolderName(`${merged.length} Local Images`);
      saveCachedFolderImages(merged);
      showToast(`Imported ${loaded.length} image files`);
    } catch (err) {
      alert('Error reading image files.');
    } finally {
      setIsLoading(false);
      setLoadingMessage('');
    }
  };

  // Load Sample Data (ADYASHA dress catalog with synthesized sample images)
  const handleLoadSampleData = () => {
    setIsLoading(true);
    setLoadingMessage('Loading ADYASHA apparel sample catalog…');
    setTimeout(() => {
      const sampleRows = generateSampleCatalog();
      const sampleImgs = generateSampleImages(sampleRows);

      setExcelRows(sampleRows);
      setExcelFileName('sample_catalog.xlsx');
      saveCachedExcelRows(sampleRows);

      setFolderImages(sampleImgs);
      setFolderName('Sample Gallery (22 Items)');
      saveCachedFolderImages(sampleImgs);

      setSelectedName('ALL');
      setSelectedColour('ALL');

      setIsLoading(false);
      setLoadingMessage('');
      showToast('Loaded ADYASHA apparel catalog with 22 items.');
    }, 300);
  };

  // Clear All Cache
  const handleClearCache = () => {
    if (confirm('Clear all cached catalog rows, images, and session data?')) {
      clearAllStorage();
      setExcelRows([]);
      setExcelFileName(null);
      setFolderImages([]);
      setFolderName(null);
      setSelectedName('ALL');
      setSelectedColour('ALL');
      showToast('All cached files and catalogs cleared.');
    }
  };

  // Filter items matching name and colour
  const displayedImages: DisplayImageItem[] = useMemo(() => {
    return matchImagesToExcelRows(excelRows, folderImages, selectedName, selectedColour);
  }, [excelRows, folderImages, selectedName, selectedColour]);

  const availableNames = useMemo(() => extractUniqueNames(excelRows), [excelRows]);
  const availableColours = useMemo(() => extractUniqueColours(excelRows), [excelRows]);

  const statusText = useMemo(() => {
    if (excelRows.length === 0) return 'No Excel catalog loaded.';
    const totalFound = displayedImages.length;
    let desc = `Showing ${totalFound} item(s)`;
    if (selectedName !== 'ALL') desc += ` for "${selectedName}"`;
    if (selectedColour !== 'ALL') desc += ` in ${selectedColour}`;
    return desc;
  }, [excelRows, displayedImages.length, selectedName, selectedColour]);

  // Modal Viewer navigation handlers
  const handleOpenModalViewer = (index: number) => {
    setModalViewerIndex(index);
    setIsSlideshowActive(false);
  };

  const handleNextModalImage = useCallback(() => {
    if (displayedImages.length === 0) return;
    setModalViewerIndex((prev) => (prev !== null ? (prev + 1) % displayedImages.length : 0));
  }, [displayedImages.length]);

  const handlePrevModalImage = useCallback(() => {
    if (displayedImages.length === 0) return;
    setModalViewerIndex((prev) =>
      prev !== null ? (prev - 1 + displayedImages.length) % displayedImages.length : 0
    );
  }, [displayedImages.length]);

  const handleToggleSlideshow = () => {
    setIsSlideshowActive((prev) => !prev);
  };

  // Test Notification action
  const handleTestNotification = () => {
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        new Notification('Excel & Image Vault', {
          body: 'Security alert test: System operational and encrypted.',
          icon: settings.customAppLogoUrl || undefined,
        });
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then((perm) => {
          if (perm === 'granted') {
            new Notification('Excel & Image Vault', {
              body: 'Notifications enabled.',
            });
          }
        });
      }
    }
    showToast('🔔 Security Alert Test: Vault operational and active.');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-indigo-500/30 selection:text-indigo-200 relative">
      {/* Privacy Masking Overlay when Anti-Screenshot is active & window is blurred */}
      {isWindowBlurred && (
        <div className="fixed inset-0 z-50 bg-slate-950/98 backdrop-blur-xl flex flex-col items-center justify-center p-6 text-center select-none pointer-events-auto">
          <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-400 mb-4 animate-pulse">
            🔒
          </div>
          <h2 className="text-xl font-bold text-slate-100">Screen Privacy Active</h2>
          <p className="text-xs text-slate-400 max-w-sm mt-1">
            Window is currently unfocused. Content is shielded from screen recording and unauthorized capture. Click back to resume.
          </p>
        </div>
      )}

      {/* Lockscreen if PIN is active and locked */}
      {isLocked && settings.isPinLockEnabled && (
        <AppLockScreen
          correctPin={settings.pinHash}
          onUnlock={() => setIsLocked(false)}
          onPinReset={(newPin) => {
            handleUpdateSettings({ ...settings, pinHash: newPin });
            showToast('PIN reset via Fingerprint Backup Dat!');
          }}
        />
      )}

      {/* Toast Notification Banner */}
      {toastMessage && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 bg-slate-900/95 border border-indigo-500/40 text-indigo-300 text-xs font-semibold px-4 py-2.5 rounded-2xl shadow-2xl backdrop-blur-md animate-fade-in flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-indigo-400 animate-ping" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Main Header Bar */}
      <HeaderBar
        excelName={excelFileName}
        folderName={folderName}
        isPermanentSaved={excelRows.length > 0}
        userProfile={userProfile}
        customLogoUrl={settings.customAppLogoUrl}
        onLoadSample={handleLoadSampleData}
        onTestNotification={handleTestNotification}
        onClearCache={handleClearCache}
        onOpenManual={() => setIsManualOpen(true)}
        onOpenProfile={() => setIsProfileOpen(true)}
      />

      {/* Primary Tab Navigation */}
      <TabNavigation
        currentTab={currentTab}
        isDualConnected={Boolean(dualSession && dualSession.isConnected)}
        isPinEnabled={settings.isPinLockEnabled}
        onTabChange={(tab) => setCurrentTab(tab)}
        onQuickLock={() => setIsLocked(true)}
      />

      {/* Active Tab Screen */}
      <main className="flex-1 flex flex-col">
        {currentTab === 'EXCEL_CATALOG' && (
          <div className="flex-1 flex flex-col">
            <ControlsSection
              hasExcel={excelRows.length > 0}
              hasFolder={folderImages.length > 0}
              selectedName={selectedName}
              selectedColour={selectedColour}
              availableNames={availableNames}
              availableColours={availableColours}
              statusText={statusText}
              isLoading={isLoading}
              loadingMessage={loadingMessage}
              onSelectExcelFile={handleSelectExcelFile}
              onSelectImageFiles={handleSelectImageFiles}
              onNameSelect={setSelectedName}
              onColourSelect={setSelectedColour}
            />

            <ImageGallery
              images={displayedImages}
              hasExcel={excelRows.length > 0}
              hasFolder={folderImages.length > 0}
              gridColumns={settings.galleryGridColumns}
              imageFitMode={settings.imageFitMode}
              highContrastBorders={settings.highContrastBorders}
              onImageClick={handleOpenModalViewer}
              onLoadSampleClick={handleLoadSampleData}
            />
          </div>
        )}

        {currentTab === 'IMAGE_VAULT' && (
          <VaultScreen
            containers={vaultContainers}
            onUpdateContainers={handleUpdateVaultContainers}
          />
        )}

        {currentTab === 'SSHOW' && (
          <SShowScreen
            containers={sshowContainers}
            onUpdateContainers={handleUpdateSShowContainers}
          />
        )}

        {currentTab === 'DUAL_VAULT' && (
          <DualVaultConnectDialog
            session={dualSession}
            onUpdateSession={handleUpdateDualSession}
          />
        )}

        {currentTab === 'SETTINGS' && (
          <SettingsScreen
            settings={settings}
            userProfile={userProfile}
            onUpdateSettings={handleUpdateSettings}
            onOpenManual={() => setIsManualOpen(true)}
            onOpenShareQr={() => setIsShareQrOpen(true)}
            onOpenLogoPicker={() => setIsLogoOpen(true)}
            onClearCache={handleClearCache}
            onOpenProfile={() => setIsProfileOpen(true)}
          />
        )}
      </main>

      {/* Fullscreen Lightbox Modal Viewer for Excel Gallery */}
      {modalViewerIndex !== null && displayedImages.length > 0 && (
        <ModalImageViewer
          images={displayedImages}
          currentIndex={modalViewerIndex}
          isSlideshowActive={isSlideshowActive}
          slideshowSpeedMs={settings.slideshowSpeedMs}
          onClose={() => {
            setModalViewerIndex(null);
            setIsSlideshowActive(false);
          }}
          onNext={handleNextModalImage}
          onPrevious={handlePrevModalImage}
          onToggleSlideshow={handleToggleSlideshow}
        />
      )}

      {/* User Manual Modal Dialog */}
      {isManualOpen && <UserManualDialog onClose={() => setIsManualOpen(false)} />}

      {/* Profile & Device ID Modal Dialog */}
      {isProfileOpen && (
        <ProfileDialog
          profile={userProfile}
          onSaveProfile={handleUpdateProfile}
          onClose={() => setIsProfileOpen(false)}
        />
      )}

      {/* Share App QR Modal Dialog */}
      {isShareQrOpen && <ShareAppQrDialog onClose={() => setIsShareQrOpen(false)} />}

      {/* App Logo Customization Dialog */}
      {isLogoOpen && (
        <AppLogoDialog
          currentLogoUrl={settings.customAppLogoUrl}
          onSaveLogo={(url) => {
            handleUpdateSettings({ ...settings, customAppLogoUrl: url });
            showToast('Custom app logo updated.');
          }}
          onClose={() => setIsLogoOpen(false)}
        />
      )}
    </div>
  );
};
export default App;
