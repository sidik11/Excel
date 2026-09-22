export interface ManualSection {
  title: string;
  iconEmoji: string;
  contents: string[];
}

export const MANUAL_SECTIONS: ManualSection[] = [
  {
    title: '1. Introduction & Overview',
    iconEmoji: '📘',
    contents: [
      'Excel & Image Vault is a dual-purpose workstation engineered for high-security media storage and spreadsheet-driven catalog searching.',
      'Key Capabilities:',
      '• Excel Catalog Search: Instant lookup and dynamic image filtering based on Excel data sheets.',
      '• Encrypted DAT Vault: AES-256 encrypted image containers (.dat) with fixed-size allocation and dummy-padding for deniability.',
      '• SShow Secure Storage: OpenSSL / CryptoJS AES-256-CBC compatible container generation and secure presentation slides.',
      '• Dual Vault Synchronization: Real-time peer-to-peer pairing using pairing codes to sync encrypted files between devices.',
      '• Multi-Layer Security: 6-Digit PIN, Master Password, Fingerprint Backup verification, and Anti-Screenshot protection.'
    ]
  },
  {
    title: '2. Security & Lockscreen Authentication',
    iconEmoji: '🛡️',
    contents: [
      'The application provides robust lockscreen protection to guard confidential photos and catalogs.',
      '• 6-Digit PIN Protection: Set a quick-access 6-digit PIN. Failed attempts are monitored to prevent brute-force attacks.',
      '• Master Password: Used as an administrative override for critical operations and PIN updates.',
      '• Biometric Fingerprint / Face Sensor: Seamless authentication using WebAuthn or device biometric sensors.',
      '• Auto-Lock Timer: Automatically locks the application when backgrounded or inactive (Immediate, 5m, 15m, 30m).',
      '• Anti-Screenshot & Screen Privacy: Enforces privacy masking when switching away or unfocusing the window.'
    ]
  },
  {
    title: '3. Fingerprint Registration & Backup File',
    iconEmoji: '🔐',
    contents: [
      'The app includes an advanced biometric recovery protocol located in Settings -> Fingerprint Registration:',
      '• Registration: Tapping "Register Fingerprint" cryptographically registers credentials and generates a signed backup file: fingerprint_backup.dat.',
      '• Storage: The backup dat file is preserved in secure persistent storage with cryptographic SHA-256 signatures.',
      '• Export: Use the "Export Fingerprint Backup (.dat)" button in Settings to transfer your fingerprint backup file to an external drive or cloud.',
      '• Cross-Device Ready: You can transfer this backup dat file to any secondary workstation or tablet to unlock the app seamlessly.'
    ]
  },
  {
    title: '4. Emergency Recovery: Unlocking with Backup Fingerprint',
    iconEmoji: '🔑',
    contents: [
      'If you forget your 6-digit PIN or need to access your vault on a newly set up device:',
      '1. On the PIN Lockscreen, click the "Use Backup Fingerprint (.dat)" button below the numeric keypad.',
      '2. Select your saved fingerprint_backup.dat file from your device.',
      '3. The app cryptographically verifies the signature and security hash.',
      '4. Once verified, the lockscreen immediately unlocks and presents an option to configure a brand-new 6-digit PIN.'
    ]
  },
  {
    title: '5. Encrypted DAT Image Vault',
    iconEmoji: '🗄️',
    contents: [
      'The Image Vault allows you to package sensitive photos into a single encrypted binary container (.dat):',
      '• Creation: Create a container with custom target size (e.g., 50 MB, 100 MB, 500 MB) padded with uniform 0xAA bytes to conceal file counts.',
      '• High-Performance Encryption Engine: AES encryption streams in memory-safe chunks, effortlessly supporting large photo collections.',
      '• Image Extraction: Unpack images to your device or view photos within the in-app lightbox.',
      '• Auto-Expansion: When importing images exceeding original capacity, the container safely auto-expands with headroom.',
      '• Portable Containers: Export or import .dat containers at any time.'
    ]
  },
  {
    title: '6. SShow (Secure Show)',
    iconEmoji: '🔒',
    contents: [
      'SShow is designed for ultra-secure presentations and confidential photo showcases:',
      '• OpenSSL / CryptoJS AES-256-CBC compatible container generation with Salted__ header.',
      '• Interactive Slideshow Viewer with customizable speeds (e.g. 1500ms, fast forward 300ms, loop).',
      '• Fullscreen light box with smooth pan, zoom, and touch gesture support.',
      '• Random 6-digit PIN generator for quick password creation and one-time sharing.'
    ]
  },
  {
    title: '7. Dual Vault Remote Sync',
    iconEmoji: '🔗',
    contents: [
      'Dual Vault lets two devices share a synchronized encrypted gallery in real time:',
      '• Host Mode: Generate a 6-character session pairing code.',
      '• Peer Mode: Enter the code on another device or browser tab to link immediately.',
      '• Live Sync: Files shared by either peer are synchronized across devices through Firebase Realtime Database.',
      '• Disconnect anytime: Either peer can tap Disconnect to terminate the live pairing.'
    ]
  },
  {
    title: '8. Profile & Mandatory Verification',
    iconEmoji: '👤',
    contents: [
      'The application features a comprehensive user verification profile:',
      '• Required Fields: Full Name, Date of Birth, Phone Number, Email, Device, Village, District, State, Country, Pincode, and Profile Picture.',
      '• Google Account Connection: Connect with Google to generate a verified 10-digit Device Profile Code (5 chars from Gmail + 2 phone digits + 3 unique digits).',
      '• Persistent Verification: Your credentials and profile code are stored locally.'
    ]
  }
];
