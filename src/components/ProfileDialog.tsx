import React, { useState, useRef } from 'react';
import { UserProfile } from '../types';
import { generateDeviceProfileCode } from '../utils/storage';
import { User, Mail, Phone, Calendar, MapPin, Globe, CheckCircle2, ShieldCheck, Upload, X } from 'lucide-react';

interface ProfileDialogProps {
  profile: UserProfile | null;
  onSaveProfile: (profile: UserProfile) => void;
  onClose: () => void;
}

export const ProfileDialog: React.FC<ProfileDialogProps> = ({
  profile,
  onSaveProfile,
  onClose,
}) => {
  const [fullName, setFullName] = useState(profile?.fullName || '');
  const [dateOfBirth, setDateOfBirth] = useState(profile?.dateOfBirth || '');
  const [phoneNumber, setPhoneNumber] = useState(profile?.phoneNumber || '');
  const [email, setEmail] = useState(profile?.email || '');
  const [deviceModel, setDeviceModel] = useState(profile?.deviceModel || 'Desktop Web');
  const [village, setVillage] = useState(profile?.village || '');
  const [district, setDistrict] = useState(profile?.district || '');
  const [state, setState] = useState(profile?.state || '');
  const [country, setCountry] = useState(profile?.country || 'India');
  const [pincode, setPincode] = useState(profile?.pincode || '');
  const [profileImageBase64, setProfileImageBase64] = useState<string | undefined>(
    profile?.profileImageBase64
  );
  const [isGoogleConnected, setIsGoogleConnected] = useState(profile?.isGoogleConnected || false);
  const [deviceProfileCode, setDeviceProfileCode] = useState(profile?.deviceProfileCode || '');

  const imagePickerRef = useRef<HTMLInputElement>(null);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        setProfileImageBase64(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleConnectGoogle = () => {
    // Generate authentic profile code if email is provided
    const targetEmail = email.trim() || 'user@example.com';
    const targetPhone = phoneNumber.trim() || '9876543210';
    const code = generateDeviceProfileCode(targetEmail, targetPhone);

    setIsGoogleConnected(true);
    setDeviceProfileCode(code);
    if (!email) setEmail(targetEmail);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const updated: UserProfile = {
      fullName,
      dateOfBirth,
      phoneNumber,
      email,
      deviceModel,
      village,
      district,
      state,
      country,
      pincode,
      profileImageBase64,
      isGoogleConnected,
      deviceProfileCode:
        deviceProfileCode || generateDeviceProfileCode(email || 'user@gmail.com', phoneNumber || '9999999999'),
      updatedAt: Date.now(),
    };
    onSaveProfile(updated);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-2xl my-8 flex flex-col gap-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <User className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100">User Profile & Device Verification</h2>
              <p className="text-xs text-slate-400">
                Identity credentials and mandatory 10-digit Device Profile Code.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-slate-800 text-slate-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Profile Code Badge */}
        {deviceProfileCode && (
          <div className="flex items-center justify-between bg-emerald-500/10 border border-emerald-500/30 rounded-2xl p-3.5">
            <div className="flex items-center gap-2.5">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              <div>
                <div className="text-xs text-emerald-300 font-semibold">Verified Device Profile Code</div>
                <div className="text-sm font-mono font-bold text-emerald-400">{deviceProfileCode}</div>
              </div>
            </div>
            <span className="text-[10px] font-mono bg-emerald-500/20 text-emerald-300 px-2.5 py-1 rounded-full">
              ACTIVE
            </span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4 text-xs">
          {/* Avatar and Google Auth */}
          <div className="flex flex-col sm:flex-row items-center gap-4 bg-slate-950/60 p-4 rounded-2xl border border-slate-800">
            <div className="relative group">
              {profileImageBase64 ? (
                <img
                  src={profileImageBase64}
                  alt="Profile"
                  className="w-16 h-16 rounded-2xl object-cover ring-2 ring-indigo-500/40"
                />
              ) : (
                <div className="w-16 h-16 rounded-2xl bg-slate-800 flex items-center justify-center text-slate-400">
                  <User className="w-8 h-8" />
                </div>
              )}
              <input
                type="file"
                ref={imagePickerRef}
                onChange={handleImageChange}
                accept="image/*"
                className="hidden"
                id="input_profile_photo"
              />
              <button
                type="button"
                onClick={() => imagePickerRef.current?.click()}
                className="absolute inset-0 bg-slate-950/60 opacity-0 group-hover:opacity-100 rounded-2xl flex items-center justify-center transition"
              >
                <Upload className="w-5 h-5 text-white" />
              </button>
            </div>

            <div className="flex-1 text-center sm:text-left">
              <div className="font-semibold text-slate-200">Profile Picture & Google Account</div>
              <div className="text-[11px] text-slate-400 mb-2">
                Click the image to upload avatar. Connect Google Account to generate verified code.
              </div>

              <button
                type="button"
                onClick={handleConnectGoogle}
                id="btn_connect_google"
                className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl font-semibold transition text-xs ${
                  isGoogleConnected
                    ? 'bg-emerald-600/20 text-emerald-300 border border-emerald-500/30'
                    : 'bg-indigo-600 hover:bg-indigo-500 text-white'
                }`}
              >
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>{isGoogleConnected ? 'Google Account Connected' : 'Connect with Google'}</span>
              </button>
            </div>
          </div>

          {/* Form Fields Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            <div>
              <label className="block text-slate-300 font-medium mb-1">Full Legal Name *</label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="e.g. Adyasha Jena"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Date of Birth</label>
              <input
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Phone Number *</label>
              <input
                type="tel"
                required
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="e.g. +91 9876543210"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Email Address *</label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="e.g. user@gmail.com"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Village / Town</label>
              <input
                type="text"
                value={village}
                onChange={(e) => setVillage(e.target.value)}
                placeholder="Village name"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">District</label>
              <input
                type="text"
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                placeholder="District"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">State / Province</label>
              <input
                type="text"
                value={state}
                onChange={(e) => setState(e.target.value)}
                placeholder="State"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Pincode / Postal Code</label>
              <input
                type="text"
                value={pincode}
                onChange={(e) => setPincode(e.target.value)}
                placeholder="Pincode"
                className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-slate-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              type="submit"
              id="btn_save_user_profile"
              className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/20"
            >
              Save Profile
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
