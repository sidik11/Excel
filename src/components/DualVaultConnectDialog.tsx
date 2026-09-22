import React, { useState, useEffect, useRef } from 'react';
import { DualVaultSession, DualVaultFileInfo } from '../types';
import {
  createHostSession,
  joinPeerSession,
  pollSessionUpdate,
  shareFileInSession,
  disconnectSession
} from '../utils/dualVaultFirebase';
import {
  Network,
  Radio,
  Copy,
  Check,
  Upload,
  FileCode,
  Download,
  Power,
  Users,
  Clock,
  HardDrive,
  RefreshCw,
  X
} from 'lucide-react';

interface DualVaultConnectDialogProps {
  session: DualVaultSession | null;
  onUpdateSession: (session: DualVaultSession | null) => void;
  onClose?: () => void;
}

export const DualVaultConnectDialog: React.FC<DualVaultConnectDialogProps> = ({
  session,
  onUpdateSession,
  onClose,
}) => {
  const [mode, setMode] = useState<'CHOICE' | 'HOST' | 'JOIN'>('CHOICE');
  const [joinCodeInput, setJoinCodeInput] = useState('');
  const [userNameInput, setUserNameInput] = useState('User Device');
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState(false);

  const fileShareInputRef = useRef<HTMLInputElement>(null);

  // Poll for session updates when connected
  useEffect(() => {
    if (!session || !session.isConnected) return;
    const interval = setInterval(async () => {
      const updates = await pollSessionUpdate(session);
      if (updates) {
        onUpdateSession({ ...session, ...updates });
      }
    }, 4000);

    return () => clearInterval(interval);
  }, [session, onUpdateSession]);

  const handleCreateHost = async () => {
    setIsProcessing(true);
    setErrorMsg(null);
    try {
      const newSession = await createHostSession(userNameInput);
      onUpdateSession(newSession);
      setMode('HOST');
    } catch (err) {
      setErrorMsg('Failed to initialize Host session.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleJoinPeer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinCodeInput.trim()) return;

    setIsProcessing(true);
    setErrorMsg(null);
    try {
      const joinedSession = await joinPeerSession(joinCodeInput, userNameInput);
      if (!joinedSession) {
        setErrorMsg('Invalid or expired session code.');
      } else {
        onUpdateSession(joinedSession);
      }
    } catch {
      setErrorMsg('Could not connect to session. Check code.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDisconnect = async () => {
    if (!session) return;
    await disconnectSession(session.code);
    onUpdateSession(null);
    setMode('CHOICE');
  };

  const handleShareFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || !e.target.files[0] || !session) return;
    const file = e.target.files[0];

    const dataUrl = await new Promise<string>((resolve) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.readAsDataURL(file);
    });

    const fileInfo: DualVaultFileInfo = {
      fileName: file.name,
      fileSizeBytes: file.size,
      addedBy: session.isHost ? session.hostName : session.peerName,
      addedTimestamp: Date.now(),
      dataUrl,
    };

    const updatedFiles = await shareFileInSession(session.code, fileInfo, session.dualVaultFiles);
    onUpdateSession({ ...session, dualVaultFiles: updatedFiles });
    e.target.value = '';
  };

  const copyCode = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  return (
    <div className="flex-1 flex flex-col bg-slate-950 p-4 md:p-6 overflow-y-auto">
      <div className="max-w-4xl mx-auto w-full flex flex-col gap-6">
        {/* Banner */}
        <div className="flex items-center justify-between bg-slate-900/80 border border-slate-800 p-4 rounded-2xl">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Network className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-slate-100">🔗 Dual Vault Connect</h2>
                <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono">
                  Live Sync
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Synchronize encrypted DAT containers and images between devices in real-time.
              </p>
            </div>
          </div>

          {onClose && (
            <button
              onClick={onClose}
              className="p-2 rounded-lg bg-slate-800 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* State 1: Connected Session */}
        {session && session.isConnected ? (
          <div className="flex flex-col gap-5">
            {/* Connection Status Card */}
            <div className="bg-slate-900/80 border border-emerald-500/30 rounded-2xl p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-lg shadow-emerald-500/5">
              <div className="flex items-center gap-3.5">
                <div className="w-3.5 h-3.5 rounded-full bg-emerald-400 animate-ping" />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-slate-100">
                      Paired Session Active
                    </span>
                    <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-emerald-400 font-bold border border-slate-700">
                      Code: {session.code}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-slate-400 mt-1">
                    <span>Host: <strong className="text-slate-200">{session.hostName}</strong></span>
                    <span>•</span>
                    <span>Peer: <strong className="text-slate-200">{session.peerName || 'Waiting…'}</strong></span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                <button
                  onClick={() => copyCode(session.code)}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
                >
                  {copiedCode ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>Copy Code</span>
                </button>

                <button
                  onClick={handleDisconnect}
                  id="btn_disconnect_dual_vault"
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-red-500/10 border border-red-500/30 text-red-400 hover:bg-red-500/20 transition active:scale-95"
                >
                  <Power className="w-3.5 h-3.5" />
                  <span>Disconnect</span>
                </button>
              </div>
            </div>

            {/* Synchronized Files Section */}
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-200">Synchronized Files ({session.dualVaultFiles.length})</h3>
                  <p className="text-xs text-slate-400">Encrypted containers shared across both paired devices.</p>
                </div>

                <div>
                  <input
                    type="file"
                    ref={fileShareInputRef}
                    onChange={handleShareFile}
                    className="hidden"
                    id="input_share_dual_vault_file"
                  />
                  <button
                    onClick={() => fileShareInputRef.current?.click()}
                    id="btn_share_dual_vault_file"
                    className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-600/20 transition active:scale-95"
                  >
                    <Upload className="w-3.5 h-3.5" />
                    <span>Share File / Container</span>
                  </button>
                </div>
              </div>

              {session.dualVaultFiles.length === 0 ? (
                <div className="p-8 text-center bg-slate-950/60 rounded-xl border border-slate-850">
                  <FileCode className="w-10 h-10 text-slate-600 mx-auto mb-2" />
                  <p className="text-xs text-slate-400">No shared files yet. Click "Share File" to sync a container.</p>
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  {session.dualVaultFiles.map((file, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between p-3 bg-slate-950/70 border border-slate-800 rounded-xl"
                    >
                      <div className="flex items-center gap-3">
                        <HardDrive className="w-4 h-4 text-emerald-400" />
                        <div>
                          <div className="text-xs font-semibold text-slate-200">{file.fileName}</div>
                          <div className="text-[11px] text-slate-400 flex items-center gap-2">
                            <span>Added by: {file.addedBy}</span>
                            <span>•</span>
                            <span>{(file.fileSizeBytes / (1024 * 1024)).toFixed(2)} MB</span>
                          </div>
                        </div>
                      </div>

                      {file.dataUrl && (
                        <a
                          href={file.dataUrl}
                          download={file.fileName}
                          className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs border border-slate-700"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download</span>
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ) : (
          /* State 2: Not Connected (Choose Host or Join) */
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Host Card */}
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 flex flex-col justify-between">
              <div>
                <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4">
                  <Radio className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-slate-100 mb-1">Host a Dual Vault Session</h3>
                <p className="text-xs text-slate-400 mb-4">
                  Generate a 6-character pairing code that another device can use to connect and sync with your vault.
                </p>

                <div className="mb-4">
                  <label className="block text-xs font-medium text-slate-300 mb-1">Your Device Identifier</label>
                  <input
                    type="text"
                    value={userNameInput}
                    onChange={(e) => setUserNameInput(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <button
                onClick={handleCreateHost}
                id="btn_host_dual_vault"
                disabled={isProcessing}
                className="w-full py-2.5 rounded-xl font-semibold text-xs bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/20 transition active:scale-95 disabled:opacity-50"
              >
                {isProcessing ? 'Generating Session…' : 'Start Host Session'}
              </button>
            </div>

            {/* Join Card */}
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 flex flex-col justify-between">
              <div>
                <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-4">
                  <Users className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-slate-100 mb-1">Join with Pairing Code</h3>
                <p className="text-xs text-slate-400 mb-4">
                  Enter the 6-character code shown on the host device to establish an encrypted bridge.
                </p>

                <form onSubmit={handleJoinPeer} className="flex flex-col gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">6-Character Pairing Code</label>
                    <input
                      type="text"
                      value={joinCodeInput}
                      onChange={(e) => setJoinCodeInput(e.target.value.toUpperCase())}
                      placeholder="e.g. 7X9K2A"
                      maxLength={8}
                      className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-center text-sm font-mono tracking-widest text-slate-100 uppercase focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>

                  {errorMsg && <p className="text-xs text-red-400 font-medium">{errorMsg}</p>}

                  <button
                    type="submit"
                    id="btn_join_dual_vault"
                    disabled={isProcessing || !joinCodeInput.trim()}
                    className="w-full mt-2 py-2.5 rounded-xl font-semibold text-xs bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-600/20 transition active:scale-95 disabled:opacity-50"
                  >
                    {isProcessing ? 'Connecting…' : 'Join Session'}
                  </button>
                </form>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
