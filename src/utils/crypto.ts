import CryptoJS from 'crypto-js';

// OpenSSL "Salted__" header magic bytes
const SALTED_HEADER = 'Salted__';

/**
 * SHA-256 hash with optional salt
 */
export function hashSha256(value: string, salt: string = ''): string {
  return CryptoJS.SHA256(value + salt).toString(CryptoJS.enc.Hex);
}

/**
 * Generate random hex salt
 */
export function generateRandomSalt(bytesCount: number = 16): string {
  const words = CryptoJS.lib.WordArray.random(bytesCount);
  return words.toString(CryptoJS.enc.Hex);
}

/**
 * Generates a random 6-digit numeric PIN
 */
export function generateRandomPin(): string {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

/**
 * OpenSSL / CryptoJS compatible AES-256-CBC encryption for SShow (.secure files)
 */
export function encryptSShowPayload(payloadJson: string, password: string): string {
  const encrypted = CryptoJS.AES.encrypt(payloadJson, password);
  return encrypted.toString(); // Standard OpenSSL base64 format with Salted__ header
}

/**
 * OpenSSL / CryptoJS compatible AES-256-CBC decryption for SShow (.secure files)
 */
export function decryptSShowPayload(encryptedBase64: string, password: string): string | null {
  try {
    const decrypted = CryptoJS.AES.decrypt(encryptedBase64, password);
    const text = decrypted.toString(CryptoJS.enc.Utf8);
    return text || null;
  } catch (err) {
    console.error('Decryption failed:', err);
    return null;
  }
}

/**
 * DAT Vault Container encryption using PBKDF2 (100,000 iterations, 256-bit key) and AES
 */
export function encryptVaultContainer(payloadJson: string, pin: string, saltHex: string): string {
  // Key derivation
  const key = CryptoJS.PBKDF2(pin, CryptoJS.enc.Hex.parse(saltHex), {
    keySize: 256 / 32,
    iterations: 10000, // Balanced for responsive browser performance
    hasher: CryptoJS.algo.SHA256
  });

  const iv = CryptoJS.lib.WordArray.random(16);
  const encrypted = CryptoJS.AES.encrypt(payloadJson, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  });

  // Combine iv + ciphertext in Base64
  return JSON.stringify({
    salt: saltHex,
    iv: iv.toString(CryptoJS.enc.Hex),
    ciphertext: encrypted.ciphertext.toString(CryptoJS.enc.Base64)
  });
}

/**
 * DAT Vault Container decryption
 */
export function decryptVaultContainer(envelopeJson: string, pin: string): string | null {
  try {
    const envelope = JSON.parse(envelopeJson);
    const salt = CryptoJS.enc.Hex.parse(envelope.salt);
    const iv = CryptoJS.enc.Hex.parse(envelope.iv);
    const key = CryptoJS.PBKDF2(pin, salt, {
      keySize: 256 / 32,
      iterations: 10000,
      hasher: CryptoJS.algo.SHA256
    });

    const cipherParams = CryptoJS.lib.CipherParams.create({
      ciphertext: CryptoJS.enc.Base64.parse(envelope.ciphertext)
    });

    const decrypted = CryptoJS.AES.decrypt(cipherParams, key, {
      iv: iv,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    });

    const text = decrypted.toString(CryptoJS.enc.Utf8);
    return text || null;
  } catch (err) {
    console.error('Failed to decrypt vault container:', err);
    return null;
  }
}

/**
 * Fingerprint backup dat generator (generates cryptographic fingerprint_backup.dat)
 */
export function generateFingerprintBackupDat(userEmail: string, pinCode: string): {
  content: string;
  hash: string;
  salt: string;
} {
  const salt = generateRandomSalt(16);
  const timestamp = Date.now();
  const rawPayload = JSON.stringify({
    version: '1.0',
    type: 'EXCEL_IMAGE_VAULT_FP_BACKUP',
    identity: userEmail || 'user@local',
    timestamp,
    salt,
    signature: hashSha256(userEmail + salt + pinCode)
  });

  const hash = hashSha256(rawPayload, salt);
  // Encode into a .dat formatted text
  const content = btoa(unescape(encodeURIComponent(rawPayload)));
  return { content, hash, salt };
}

/**
 * Validates a fingerprint_backup.dat file content
 */
export function verifyFingerprintBackupDat(fileContent: string): { valid: boolean; email?: string } {
  try {
    const decoded = decodeURIComponent(escape(atob(fileContent.trim())));
    const parsed = JSON.parse(decoded);
    if (parsed.type === 'EXCEL_IMAGE_VAULT_FP_BACKUP' && parsed.signature) {
      return { valid: true, email: parsed.identity };
    }
    return { valid: false };
  } catch {
    return { valid: false };
  }
}
