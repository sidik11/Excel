# Excel & Image Vault (Web Edition)

A high-performance React & TypeScript web application rewritten from the original Android application, preserving all core capabilities:

- **📊 Excel Catalog Search**: Instant lookup and dynamic image filtering based on Excel spreadsheets (`.xlsx`, `.xls`, `.csv`). Filter by Name and Color with responsive grid layouts.
- **🔐 Encrypted DAT Image Vault**: AES-256 PBKDF2 binary containers (`.dat`) with fixed-size allocation and dummy-padding for plausible deniability. Add, view, extract to ZIP, and export `.dat` containers.
- **🛡️ SShow (Secure Show)**: OpenSSL AES-256-CBC compatible encrypted presentation slideshows (`.secure`) with random PIN generation, fast-forward support, and fullscreen lightbox.
- **🔗 Dual Vault Remote Sync**: Live pairing between devices using 6-character session codes to synchronize encrypted files over Firebase Realtime Database.
- **🔒 Multi-Layer Security**: 6-Digit PIN lockscreen, Biometric sensor simulation, and emergency recovery via `fingerprint_backup.dat` files.
- **⚙️ Comprehensive Settings**: Grid column selector (2-5 cols), high-contrast borders, anti-screenshot privacy window blur masking, auto-lock timer, custom app logo, and User Operations Manual.

## Getting Started

```bash
npm install
npm run dev
```

Built with React 18, Vite, TypeScript, Tailwind CSS, Lucide icons, CryptoJS, and XLSX.
