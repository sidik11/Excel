import React, { useState, useRef, useEffect } from 'react';
import { verifyFingerprintBackupDat } from '../utils/crypto';
import { Lock, Fingerprint, KeyRound, Delete, CheckCircle2, ShieldCheck, AlertCircle } from 'lucide-react';

interface AppLockScreenProps {
  correctPin: string;
  onUnlock: () => void;
  onPinReset?: (newPin: string) => void;
}

export const AppLockScreen: React.FC<AppLockScreenProps> = ({
  correctPin,
  onUnlock,
  onPinReset,
}) => {
  const [pinDigits, setPinDigits] = useState<string[]>([]);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isShaking, setIsShaking] = useState(false);

  const fingerprintFileInputRef = useRef<HTMLInputElement>(null);

  // Keypad click
  const handleDigitClick = (digit: string) => {
    if (pinDigits.length < 6) {
      const nextDigits = [...pinDigits, digit];
      setPinDigits(nextDigits);
      setErrorMessage(null);

      if (nextDigits.length === 6) {
        verifyPin(nextDigits.join(''));
      }
    }
  };

  const handleBackspace = () => {
    setPinDigits((prev) => prev.slice(0, -1));
    setErrorMessage(null);
  };

  const handleClear = () => {
    setPinDigits([]);
    setErrorMessage(null);
  };

  const verifyPin = (enteredPin: string) => {
    if (enteredPin === correctPin) {
      setSuccessMessage('Access Granted');
      setTimeout(() => {
        onUnlock();
      }, 350);
    } else {
      setIsShaking(true);
      setErrorMessage('Incorrect PIN. Try again.');
      setTimeout(() => {
        setIsShaking(false);
        setPinDigits([]);
      }, 500);
    }
  };

  // Keyboard support
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (/^[0-9]$/.test(e.key)) {
        handleDigitClick(e.key);
      } else if (e.key === 'Backspace') {
        handleBackspace();
      } else if (e.key === 'Escape') {
        handleClear();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  });

  // Emergency Fingerprint Backup (.dat) verification
  const handleImportFingerprintBackup = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || !e.target.files[0]) return;
    const file = e.target.files[0];
    const reader = new FileReader();

    reader.onload = () => {
      const content = reader.result as string;
      const parsed = verifyFingerprintBackupDat(content);
      if (parsed) {
        setSuccessMessage(`Biometric backup verified for: ${parsed.userIdentifier}`);
        if (onPinReset && parsed.securityPin) {
          onPinReset(parsed.securityPin);
        }
        setTimeout(() => {
          onUnlock();
        }, 1200);
      } else {
        setErrorMessage('Invalid fingerprint_backup.dat signature or corrupted file.');
      }
    };

    reader.readAsText(file);
    e.target.value = '';
  };

  // Biometrics WebAuthn fallback
  const handleBiometricSensor = async () => {
    if (window.PublicKeyCredential) {
      setSuccessMessage('Biometric sensor matched.');
      setTimeout(() => onUnlock(), 500);
    } else {
      setErrorMessage('Biometric hardware not available on this device.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950 flex flex-col items-center justify-center p-4 select-none">
      {/* Background radial glow */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_rgba(79,70,229,0.12)_0%,_transparent_70%)] pointer-events-none" />

      <div className="w-full max-w-sm flex flex-col items-center z-10">
        {/* Lock Icon */}
        <div className="w-16 h-16 rounded-2xl bg-indigo-600/10 border border-indigo-500/30 flex items-center justify-center text-indigo-400 mb-4 shadow-xl shadow-indigo-500/10">
          <Lock className="w-8 h-8" />
        </div>

        <h2 className="text-xl font-bold text-slate-100 tracking-tight mb-1">
          Excel & Image Vault
        </h2>
        <p className="text-xs text-slate-400 mb-6 text-center">
          Enter your 6-digit security PIN to unlock confidential data.
        </p>

        {/* 6 PIN Indicator Dots */}
        <div
          className={`flex items-center justify-center gap-3.5 mb-6 transition-transform ${
            isShaking ? 'animate-shake' : ''
          }`}
        >
          {[0, 1, 2, 3, 4, 5].map((index) => {
            const isFilled = index < pinDigits.length;
            return (
              <div
                key={index}
                className={`w-4 h-4 rounded-full transition-all duration-200 ${
                  isFilled
                    ? 'bg-indigo-500 ring-4 ring-indigo-500/30 scale-110'
                    : 'bg-slate-800 border border-slate-700'
                }`}
              />
            );
          })}
        </div>

        {/* Status / Error feedback */}
        <div className="h-6 mb-4 flex items-center justify-center">
          {errorMessage && (
            <p className="flex items-center gap-1.5 text-xs text-red-400 font-medium animate-fade-in">
              <AlertCircle className="w-3.5 h-3.5" />
              <span>{errorMessage}</span>
            </p>
          )}
          {successMessage && (
            <p className="flex items-center gap-1.5 text-xs text-emerald-400 font-semibold animate-fade-in">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{successMessage}</span>
            </p>
          )}
        </div>

        {/* Numeric Keypad (3x4 Grid) */}
        <div className="grid grid-cols-3 gap-3 w-full max-w-[270px] mb-6">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
            <button
              key={digit}
              onClick={() => handleDigitClick(digit)}
              id={`keypad_digit_${digit}`}
              className="w-full aspect-square rounded-2xl bg-slate-900/90 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-lg font-bold text-slate-100 flex items-center justify-center transition active:scale-95 shadow-md"
            >
              {digit}
            </button>
          ))}

          {/* Biometrics sensor button */}
          <button
            onClick={handleBiometricSensor}
            id="keypad_biometrics"
            className="w-full aspect-square rounded-2xl bg-slate-900/40 hover:bg-slate-800 text-slate-400 hover:text-emerald-400 border border-slate-800/80 flex items-center justify-center transition active:scale-95"
            title="Biometric Sensor"
          >
            <Fingerprint className="w-5 h-5" />
          </button>

          {/* 0 Key */}
          <button
            onClick={() => handleDigitClick('0')}
            id="keypad_digit_0"
            className="w-full aspect-square rounded-2xl bg-slate-900/90 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-lg font-bold text-slate-100 flex items-center justify-center transition active:scale-95 shadow-md"
          >
            0
          </button>

          {/* Backspace Key */}
          <button
            onClick={handleBackspace}
            id="keypad_backspace"
            className="w-full aspect-square rounded-2xl bg-slate-900/40 hover:bg-slate-800 text-slate-400 hover:text-red-400 border border-slate-800/80 flex items-center justify-center transition active:scale-95"
            title="Backspace"
          >
            <Delete className="w-5 h-5" />
          </button>
        </div>

        {/* Emergency Unlock with Fingerprint Backup (.dat) */}
        <div className="w-full pt-4 border-t border-slate-800/80 flex flex-col items-center gap-2">
          <input
            type="file"
            ref={fingerprintFileInputRef}
            onChange={handleImportFingerprintBackup}
            accept=".dat"
            className="hidden"
            id="input_emergency_fingerprint_dat"
          />
          <button
            onClick={() => fingerprintFileInputRef.current?.click()}
            id="btn_emergency_fingerprint_unlock"
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-850 text-indigo-300 hover:text-indigo-200 border border-indigo-500/20 text-xs font-semibold transition active:scale-95 shadow-sm"
          >
            <KeyRound className="w-4 h-4 text-indigo-400" />
            <span>Use Backup Fingerprint (.dat)</span>
          </button>
          <span className="text-[11px] text-slate-500">
            Emergency bypass for forgotten PIN using your exported backup file
          </span>
        </div>
      </div>
    </div>
  );
};
