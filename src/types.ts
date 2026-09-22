// Core Application TypeScript Interfaces for Excel & Image Vault

export type MainAppTab = 'EXCEL_CATALOG' | 'IMAGE_VAULT' | 'SSHOW' | 'DUAL_VAULT' | 'SETTINGS';

export interface AppSettings {
  galleryGridColumns: number; // 2, 3, 4, 5
  slideshowSpeedMs: number;
  highContrastBorders: boolean;
  imageFitMode: 'fit' | 'crop';
  antiScreenshotProtection: boolean;
  autoLockMinutes: number; // 0 = Immediate, 5, 15, 30, -1 = Never
  isPinLockEnabled: boolean;
  pinHash: string; // 6-digit PIN
  fingerprintRegistered: boolean;
  fingerprintBackupDatBase64?: string;
  customAppLogoUrl?: string;
}

export interface UserProfile {
  fullName: string;
  dateOfBirth: string;
  phoneNumber: string;
  email: string;
  deviceModel: string;
  village: string;
  district: string;
  state: string;
  country: string;
  pincode: string;
  profileImageBase64?: string;
  isGoogleConnected: boolean;
  deviceProfileCode: string; // 10-character verified code
  updatedAt: number;
}

export interface ExcelRowItem {
  code: string;
  name: string;
  colour: string;
  rowNumber?: number;
}

export interface LoadedFolderImage {
  fileName: string;
  dataUrl: string;
}

export interface DisplayImageItem {
  id: string;
  code: string;
  name: string;
  colour: string;
  fileName: string;
  fileUri: string;
  rowNumber?: number;
}

export interface VaultImage {
  id: string;
  name: string;
  dataUrl: string;
  sizeBytes: number;
  addedAt: number;
}

export interface VaultContainerInfo {
  id: string;
  fileName: string;
  sizeBytes: number;
  targetSizeBytes: number;
  allocatedMb: number;
  isUnlocked: boolean;
  images: VaultImage[];
  encryptedData?: string;
  createdAt: number;
  updatedAt: number;
}

export interface SShowImage {
  id: string;
  name: string;
  dataUrl: string;
  sizeBytes: number;
  addedAt: number;
}

export interface SShowContainer {
  id: string;
  fileName: string;
  salt: string;
  encryptedBlobBase64?: string;
  images: SShowImage[];
  passwordHint?: string;
  isUnlocked: boolean;
  createdAt: number;
}

export interface DualVaultFileInfo {
  fileName: string;
  fileSizeBytes: number;
  addedBy: string;
  addedTimestamp: number;
  dataUrl?: string;
}

export interface DualVaultSession {
  code: string;
  sessionId: string;
  isHost: boolean;
  isConnected: boolean;
  status: 'IDLE' | 'WAITING' | 'CONNECTED' | 'DISCONNECTED';
  hostName: string;
  peerName: string;
  connectedAt: number;
  dualVaultFiles: DualVaultFileInfo[];
  lastSyncTimestamp: number;
  isSyncing: boolean;
}
