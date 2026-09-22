import * as XLSX from 'xlsx';
import { ExcelRowItem, LoadedFolderImage, DisplayImageItem } from '../types';

/**
 * Normalizes a color string for consistent matching.
 */
export function normalizeColour(value: string): string {
  return value.trim().toLowerCase().replace(/\s+/g, ' ');
}

/**
 * Splits compound color strings such as "APRICOT/YELLOW" or "RED, BLUE" into individual colors.
 */
export function splitColours(value: string): string[] {
  return value
    .split(/[/,&+|]+/)
    .map(c => normalizeColour(c))
    .filter(c => c.length > 0);
}

/**
 * Normalizes a code for matching (e.g. "ADYASHA", "adyasha", "ady_asha", "ADY-ASHA" -> "adyasha").
 */
export function normalizeCode(value: string): string {
  return value.trim().toLowerCase().replace(/[ _-]+/g, '');
}

/**
 * Parses an Excel (.xlsx, .xls) or CSV file ArrayBuffer.
 */
export function parseExcelArrayBuffer(data: ArrayBuffer): ExcelRowItem[] {
  try {
    const workbook = XLSX.read(data, { type: 'array' });
    const firstSheetName = workbook.SheetNames[0];
    if (!firstSheetName) return [];

    const sheet = workbook.Sheets[firstSheetName];
    const rawRows: (string | number | undefined)[][] = XLSX.utils.sheet_to_json(sheet, {
      header: 1,
      defval: '',
      blankrows: false
    });

    if (rawRows.length < 2) return [];

    const headerRow = rawRows[0].map(cell => String(cell || '').trim().toUpperCase().replace(/\s+/g, ' '));

    let codeCol = -1;
    let nameCol = -1;
    let colourCol = -1;

    for (let idx = 0; idx < headerRow.length; idx++) {
      const norm = headerRow[idx];
      if (norm === 'CODE' && codeCol === -1) codeCol = idx;
      if (norm === 'NAME' && nameCol === -1) nameCol = idx;
      if (['TOP COLOR', 'TOP COLOUR', 'COLOUR', 'COLOR'].includes(norm) && colourCol === -1) {
        colourCol = idx;
      }
    }

    // Default column index fallbacks
    if (codeCol === -1) codeCol = 1;
    if (nameCol === -1) nameCol = 2;
    if (colourCol === -1) colourCol = 4;

    const result: ExcelRowItem[] = [];

    for (let i = 1; i < rawRows.length; i++) {
      const row = rawRows[i];
      const code = String(row[codeCol] || '').trim();
      const name = String(row[nameCol] || '').trim();
      const colour = String(row[colourCol] || '').trim();

      if (code && name) {
        result.push({
          code,
          name,
          colour,
          rowNumber: i + 1
        });
      }
    }

    return result;
  } catch (error) {
    console.error('Error parsing excel buffer:', error);
    return [];
  }
}

/**
 * Generates sample catalog rows matching original ExcelParser.kt
 */
export function generateSampleCatalog(): ExcelRowItem[] {
  return [
    { code: 'ADY-001', name: 'ADYASHA', colour: 'NAVY BLUE', rowNumber: 2 },
    { code: 'ADY-002', name: 'ADYASHA', colour: 'APRICOT/YELLOW', rowNumber: 3 },
    { code: 'ADY-003', name: 'ADYASHA', colour: 'ROSE PINK', rowNumber: 4 },
    { code: 'ADY-004', name: 'ADYASHA', colour: 'EMERALD GREEN', rowNumber: 5 },
    { code: 'ADY-005', name: 'ADYASHA', colour: 'BLACK', rowNumber: 6 },
    { code: 'ADY-006', name: 'ADYASHA', colour: 'BURGUNDY', rowNumber: 7 },
    { code: 'FLR-101', name: 'FLORA', colour: 'ROSE PINK', rowNumber: 8 },
    { code: 'FLR-102', name: 'FLORA', colour: 'WHITE/GOLD', rowNumber: 9 },
    { code: 'FLR-103', name: 'FLORA', colour: 'LAVENDER', rowNumber: 10 },
    { code: 'FLR-104', name: 'FLORA', colour: 'SKY BLUE', rowNumber: 11 },
    { code: 'SLK-201', name: 'SILK BREEZE', colour: 'ROYAL BLUE', rowNumber: 12 },
    { code: 'SLK-202', name: 'SILK BREEZE', colour: 'APRICOT/YELLOW', rowNumber: 13 },
    { code: 'SLK-203', name: 'SILK BREEZE', colour: 'PEACH', rowNumber: 14 },
    { code: 'SLK-204', name: 'SILK BREEZE', colour: 'EMERALD GREEN', rowNumber: 15 },
    { code: 'VNT-301', name: 'VINTAGE GLAM', colour: 'BLACK', rowNumber: 16 },
    { code: 'VNT-302', name: 'VINTAGE GLAM', colour: 'WHITE/GOLD', rowNumber: 17 },
    { code: 'VNT-303', name: 'VINTAGE GLAM', colour: 'BURGUNDY', rowNumber: 18 },
    { code: 'VNT-304', name: 'VINTAGE GLAM', colour: 'NAVY BLUE', rowNumber: 19 },
    { code: 'AUR-401', name: 'AURORA', colour: 'LAVENDER', rowNumber: 20 },
    { code: 'AUR-402', name: 'AURORA', colour: 'SKY BLUE', rowNumber: 21 },
    { code: 'AUR-403', name: 'AURORA', colour: 'CORAL', rowNumber: 22 },
    { code: 'AUR-404', name: 'AURORA', colour: 'MINT GREEN', rowNumber: 23 }
  ];
}

/**
 * Creates high quality SVG visual data URLs for catalog sample images
 */
export function generateSampleImageSvg(code: string, name: string, colour: string): string {
  const colorMap: Record<string, [string, string]> = {
    'navy blue': ['#0f172a', '#1e3a8a'],
    'apricot/yellow': ['#f59e0b', '#fbbf24'],
    'rose pink': ['#db2777', '#f472b6'],
    'emerald green': ['#047857', '#10b981'],
    'black': ['#18181b', '#27272a'],
    'burgundy': ['#881337', '#be123c'],
    'white/gold': ['#d97706', '#fef08a'],
    'lavender': ['#7c3aed', '#c084fc'],
    'sky blue': ['#0284c7', '#38bdf8'],
    'royal blue': ['#1d4ed8', '#3b82f6'],
    'peach': ['#ea580c', '#fb923c'],
    'coral': ['#e11d48', '#fb7185'],
    'mint green': ['#0d9488', '#2dd4bf']
  };

  const key = colour.toLowerCase();
  const colors = colorMap[key] || ['#3b82f6', '#8b5cf6'];

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600">
    <defs>
      <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${colors[0]}" />
        <stop offset="100%" stop-color="${colors[1]}" />
      </linearGradient>
      <filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">
        <feDropShadow dx="0" dy="8" stdDeviation="12" flood-opacity="0.35"/>
      </filter>
    </defs>
    <rect width="600" height="600" fill="#090d16" />
    <circle cx="300" cy="240" r="160" fill="url(#bgGrad)" opacity="0.9" filter="url(#shadow)" />
    <!-- Fabric / Catalog drape silhouette illustration -->
    <path d="M 230 140 C 270 120, 330 120, 370 140 C 410 180, 420 320, 390 380 C 340 400, 260 400, 210 380 C 180 320, 190 180, 230 140 Z" fill="#ffffff" opacity="0.18" />
    <path d="M 250 170 C 280 150, 320 150, 350 170 C 370 230, 370 330, 340 360 C 310 375, 290 375, 260 360 C 230 330, 230 230, 250 170 Z" fill="#ffffff" opacity="0.22" />
    
    <!-- Badges -->
    <rect x="40" y="40" width="130" height="34" rx="17" fill="rgba(15, 23, 42, 0.75)" stroke="rgba(255,255,255,0.2)" stroke-width="1"/>
    <text x="105" y="62" fill="#38bdf8" font-family="monospace" font-size="13" font-weight="700" text-anchor="middle">${code}</text>

    <!-- Details Overlay -->
    <rect x="30" y="470" width="540" height="96" rx="16" fill="rgba(15, 23, 42, 0.85)" stroke="rgba(255,255,255,0.15)" stroke-width="1" />
    <text x="50" y="508" fill="#f8fafc" font-family="system-ui, sans-serif" font-size="22" font-weight="bold">${name}</text>
    <text x="50" y="542" fill="#94a3b8" font-family="system-ui, sans-serif" font-size="14">COLOR: <tspan fill="#38bdf8" font-weight="bold">${colour}</tspan></text>
    <circle cx="530" cy="518" r="16" fill="${colors[0]}" stroke="#ffffff" stroke-width="2" />
  </svg>`;

  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg);
}

/**
 * Creates loaded folder image objects for sample rows
 */
export function generateSampleImages(rows: ExcelRowItem[]): LoadedFolderImage[] {
  return rows.map((row) => ({
    fileName: `${row.code}_${row.name.replace(/\s+/g, '_')}.png`,
    dataUrl: generateSampleImageSvg(row.code, row.name, row.colour)
  }));
}

/**
 * Matches folder images against Excel catalog rows and active filter selections.
 */
export function matchImagesToExcelRows(
  rows: ExcelRowItem[],
  folderImages: LoadedFolderImage[],
  selectedName: string,
  selectedColour: string
): DisplayImageItem[] {
  // If no Excel rows loaded, treat folder images as raw gallery
  if (rows.length === 0) {
    return folderImages.map((img, idx) => ({
      id: `img_${idx}`,
      code: `IMG-${idx + 1}`,
      name: img.fileName.replace(/\.[^/.]+$/, ''),
      colour: 'DEFAULT',
      fileName: img.fileName,
      fileUri: img.dataUrl,
    }));
  }

  // Filter rows by selectedName and selectedColour
  const filteredRows = rows.filter((row) => {
    if (selectedName !== 'ALL' && row.name.toUpperCase() !== selectedName.toUpperCase()) {
      return false;
    }
    if (selectedColour !== 'ALL') {
      const targetColors = splitColours(selectedColour);
      const rowColors = splitColours(row.colour);
      const hasMatch = targetColors.some(tc => rowColors.includes(tc));
      if (!hasMatch) return false;
    }
    return true;
  });

  const results: DisplayImageItem[] = [];

  // Match folder images to filtered rows
  filteredRows.forEach((row, idx) => {
    const normCode = normalizeCode(row.code);
    const normName = normalizeCode(row.name);

    // Look for matching file in folderImages
    const matchedImage = folderImages.find((img) => {
      const cleanFile = normalizeCode(img.fileName);
      return cleanFile.includes(normCode) || (normCode.length > 3 && cleanFile.startsWith(normCode));
    });

    if (matchedImage) {
      results.push({
        id: `matched_${row.code}_${idx}`,
        code: row.code,
        name: row.name,
        colour: row.colour,
        fileName: matchedImage.fileName,
        fileUri: matchedImage.dataUrl,
        rowNumber: row.rowNumber
      });
    } else {
      // If no folder image directly matched, provide the stylized SVG thumbnail
      results.push({
        id: `synth_${row.code}_${idx}`,
        code: row.code,
        name: row.name,
        colour: row.colour,
        fileName: `${row.code}.svg`,
        fileUri: generateSampleImageSvg(row.code, row.name, row.colour),
        rowNumber: row.rowNumber
      });
    }
  });

  return results;
}

/**
 * Extracts unique Names with count
 */
export function extractUniqueNames(rows: ExcelRowItem[]): { name: string; count: number }[] {
  const map = new Map<string, number>();
  rows.forEach((r) => {
    const n = r.name.trim();
    if (n) {
      map.set(n, (map.get(n) || 0) + 1);
    }
  });
  return Array.from(map.entries()).map(([name, count]) => ({ name, count }));
}

/**
 * Extracts unique Colours with count
 */
export function extractUniqueColours(rows: ExcelRowItem[]): { colour: string; count: number }[] {
  const map = new Map<string, number>();
  rows.forEach((r) => {
    const c = r.colour.trim().toUpperCase();
    if (c) {
      map.set(c, (map.get(c) || 0) + 1);
    }
  });
  return Array.from(map.entries()).map(([colour, count]) => ({ colour, count }));
}
