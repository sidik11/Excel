import React, { useRef } from 'react';
import { FileSpreadsheet, FolderPlus, Filter, Palette, Loader2, CheckCircle2 } from 'lucide-react';

interface ControlsSectionProps {
  hasExcel: boolean;
  hasFolder: boolean;
  selectedName: string;
  selectedColour: string;
  availableNames: { name: string; count: number }[];
  availableColours: { colour: string; count: number }[];
  statusText: string;
  isLoading: boolean;
  loadingMessage: string;
  onSelectExcelFile: (file: File) => void;
  onSelectImageFiles: (files: FileList) => void;
  onNameSelect: (name: string) => void;
  onColourSelect: (colour: string) => void;
}

export const ControlsSection: React.FC<ControlsSectionProps> = ({
  hasExcel,
  hasFolder,
  selectedName,
  selectedColour,
  availableNames,
  availableColours,
  statusText,
  isLoading,
  loadingMessage,
  onSelectExcelFile,
  onSelectImageFiles,
  onNameSelect,
  onColourSelect,
}) => {
  const excelInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  const handleExcelChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      onSelectExcelFile(e.target.files[0]);
    }
  };

  const handleImagesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      onSelectImageFiles(e.target.files);
    }
  };

  return (
    <div className="w-full bg-slate-900/50 border-b border-slate-800/80 px-4 py-3">
      <div className="max-w-7xl mx-auto flex flex-col gap-3">
        {/* Row 1: File Pickers & Dropdowns */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          {/* File Picker Buttons */}
          <div className="flex flex-wrap items-center gap-2">
            <input
              type="file"
              ref={excelInputRef}
              onChange={handleExcelChange}
              accept=".xlsx,.xls,.csv"
              className="hidden"
              id="input_excel_file"
            />
            <button
              onClick={() => excelInputRef.current?.click()}
              id="btn_select_excel"
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold border transition active:scale-95 shadow-sm ${
                hasExcel
                  ? 'bg-emerald-600/20 border-emerald-500/40 text-emerald-300 hover:bg-emerald-600/30'
                  : 'bg-indigo-600 hover:bg-indigo-500 text-white border-transparent'
              }`}
            >
              <FileSpreadsheet className="w-4 h-4" />
              <span>{hasExcel ? 'Replace Excel' : 'Select Excel (.xlsx)'}</span>
              {hasExcel && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />}
            </button>

            <input
              type="file"
              ref={folderInputRef}
              onChange={handleImagesChange}
              multiple
              accept="image/*"
              className="hidden"
              id="input_folder_images"
            />
            <button
              onClick={() => folderInputRef.current?.click()}
              id="btn_select_folder"
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold border transition active:scale-95 shadow-sm ${
                hasFolder
                  ? 'bg-cyan-600/20 border-cyan-500/40 text-cyan-300 hover:bg-cyan-600/30'
                  : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
              }`}
            >
              <FolderPlus className="w-4 h-4" />
              <span>{hasFolder ? 'Add More Images' : 'Select Images / Folder'}</span>
              {hasFolder && <CheckCircle2 className="w-3.5 h-3.5 text-cyan-400" />}
            </button>
          </div>

          {/* Filter Dropdowns */}
          <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            {/* Name Filter */}
            <div className="relative flex-1 sm:flex-none min-w-[160px]">
              <div className="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
                <Filter className="w-3.5 h-3.5" />
              </div>
              <select
                value={selectedName}
                onChange={(e) => onNameSelect(e.target.value)}
                id="select_name_filter"
                className="w-full pl-8 pr-7 py-2 bg-slate-800/90 hover:bg-slate-800 text-slate-100 text-xs rounded-xl border border-slate-700/80 focus:ring-2 focus:ring-indigo-500 focus:outline-none transition appearance-none cursor-pointer"
              >
                <option value="ALL">All Names ({availableNames.reduce((acc, n) => acc + n.count, 0)})</option>
                {availableNames.map((item) => (
                  <option key={item.name} value={item.name}>
                    {item.name} ({item.count})
                  </option>
                ))}
              </select>
            </div>

            {/* Colour Filter */}
            <div className="relative flex-1 sm:flex-none min-w-[160px]">
              <div className="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
                <Palette className="w-3.5 h-3.5" />
              </div>
              <select
                value={selectedColour}
                onChange={(e) => onColourSelect(e.target.value)}
                id="select_colour_filter"
                className="w-full pl-8 pr-7 py-2 bg-slate-800/90 hover:bg-slate-800 text-slate-100 text-xs rounded-xl border border-slate-700/80 focus:ring-2 focus:ring-indigo-500 focus:outline-none transition appearance-none cursor-pointer uppercase"
              >
                <option value="ALL">All Colours ({availableColours.reduce((acc, c) => acc + c.count, 0)})</option>
                {availableColours.map((item) => (
                  <option key={item.colour} value={item.colour}>
                    {item.colour} ({item.count})
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* Row 2: Status Bar and Loading info */}
        <div className="flex items-center justify-between text-xs text-slate-400 pt-1">
          <div className="flex items-center gap-2">
            {isLoading ? (
              <span className="flex items-center gap-2 text-indigo-400 font-medium">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>{loadingMessage || 'Processing…'}</span>
              </span>
            ) : (
              <span className="font-medium text-slate-300">
                {statusText || 'Ready. Choose an Excel sheet or load sample catalog.'}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
