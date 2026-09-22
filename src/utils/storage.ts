import {
  AppSettings,
  UserProfile,
  ExcelRowItem,
  LoadedFolderImage,
  VaultContainerInfo,
  SShowContainer
} from '../types';

const STORAGE_KEYS = {
  SETTINGS: 'eiv_app_settings',
  PROFILE: 'eiv_user_profile',
  EXCEL_ROWS: 'eiv_excel_rows',
  FOLDER_IMAGES: 'eiv_folder_images',
  VAULT_CONTAINERS: 'eiv_vault_containers',
  SSHOW_CONTAINERS: 'eiv_sshow_containers'
};

export const DEFAULT_SETTINGS: AppSettings = {
  galleryGridColumns: 3,
  slideshowSpeedMs: 1500,
  highContrastBorders: true,
  imageFitMode: 'fit',
  antiScreenshotProtection: false,
  autoLockMinutes: 0,
  isPinLockEnabled: false,
  pinHash: '123456',
  fingerprintRegistered: false,
  customAppLogoUrl: ''
};

export function loadAppSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.SETTINGS);
    if (!raw) return DEFAULT_SETTINGS;
    return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function saveAppSettings(settings: AppSettings): void {
  try {
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
  } catch (err) {
    console.error('Error saving settings', err);
  }
}

export function loadUserProfile(): UserProfile | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.PROFILE);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function saveUserProfile(profile: UserProfile): void {
  try {
    localStorage.setItem(STORAGE_KEYS.PROFILE, JSON.stringify(profile));
  } catch (err) {
    console.error('Error saving user profile', err);
  }
}

/**
 * Generates verified 10-character Device Profile Code:
 * 5 chars from Gmail + 2 phone digits + 3 unique digits
 */
export function generateDeviceProfileCode(email: string, phone: string): string {
  const cleanEmail = email.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
  const emailPart = (cleanEmail.padEnd(5, 'X')).substring(0, 5);

  const cleanPhone = phone.replace(/\D/g, '');
  const phonePart = cleanPhone.length >= 2 ? cleanPhone.slice(-2) : '99';

  const randomPart = Math.floor(100 + Math.random() * 900).toString();
  return `${emailPart}${phonePart}${randomPart}`;
}

export function loadCachedExcelRows(): ExcelRowItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.EXCEL_ROWS);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveCachedExcelRows(rows: ExcelRowItem[]): void {
  try {
    localStorage.setItem(STORAGE_KEYS.EXCEL_ROWS, JSON.stringify(rows));
  } catch (err) {
    console.error('Error saving excel cache', err);
  }
}

export function loadCachedFolderImages(): LoadedFolderImage[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.FOLDER_IMAGES);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveCachedFolderImages(images: LoadedFolderImage[]): void {
  try {
    // Only cache if payload is under standard quota limit (~4MB)
    const json = JSON.stringify(images);
    if (json.length < 4 * 1024 * 1024) {
      localStorage.setItem(STORAGE_KEYS.FOLDER_IMAGES, json);
    }
  } catch (err) {
    console.warn('Image cache skipped due to storage quota', err);
  }
}

export function loadVaultContainers(): VaultContainerInfo[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.VAULT_CONTAINERS);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveVaultContainers(containers: VaultContainerInfo[]): void {
  try {
    localStorage.setItem(STORAGE_KEYS.VAULT_CONTAINERS, JSON.stringify(containers));
  } catch (err) {
    console.error('Error saving vault containers', err);
  }
}

export function loadSShowContainers(): SShowContainer[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.SSHOW_CONTAINERS);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveSShowContainers(containers: SShowContainer[]): void {
  try {
    localStorage.setItem(STORAGE_KEYS.SSHOW_CONTAINERS, JSON.stringify(containers));
  } catch (err) {
    console.error('Error saving sshow containers', err);
  }
}

export function clearAllStorage(): void {
  try {
    localStorage.removeItem(STORAGE_KEYS.EXCEL_ROWS);
    localStorage.removeItem(STORAGE_KEYS.FOLDER_IMAGES);
  } catch (err) {
    console.error('Error clearing storage', err);
  }
}
