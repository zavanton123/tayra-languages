/** How subtitles may be hidden until revealed: by hovering them or pressing E. */
export type HideMode = 'off' | 'translation' | 'subtitle' | 'translation-then-subtitle' | 'subtitle-then-translation';

export interface ExtensionSettings {
  enabled: boolean;
  autoPause: boolean;
  hideMode: HideMode;
  /** 0 to 4: extra small to extra large. */
  fontSize: number;
  font: 'default' | 'sans' | 'serif';
  /** The subtitles' background, 0 to 100. */
  backgroundOpacity: number;
  /** How high the subtitles sit, in % of the player's height from its bottom. */
  position: number;
  showControls: boolean;
  /** YouTube's own machine translation of the track. */
  machineTranslation: boolean;
  /** Subtitles a person made in the translation language, when the video has them. */
  humanTranslation: boolean;
  /** The language translations are in; null for the native language set in Tayra. */
  translationLanguage: string | null;
  /** Arrows move between lines too, instead of YouTube's 5-second jumps. */
  arrowKeys: boolean;
  /** Pause at the end of the line while the pointer is on the subtitles. */
  hoverHold: boolean;
}

export const DEFAULT_SETTINGS: ExtensionSettings = {
  enabled: true,
  autoPause: false,
  hideMode: 'off',
  fontSize: 2,
  font: 'default',
  backgroundOpacity: 70,
  position: 11,
  showControls: true,
  machineTranslation: true,
  humanTranslation: false,
  translationLanguage: null,
  arrowKeys: false,
  hoverHold: true,
};

export const FONT_SCALES = [0.7, 0.85, 1, 1.15, 1.35];
export const POSITION_RANGE = { min: 3, max: 80 };

/** Settings as saved, with anything missing or out of range back to its default. */
export function normalize(saved: unknown): ExtensionSettings {
  const s = { ...DEFAULT_SETTINGS, ...(typeof saved === 'object' && saved ? (saved as Partial<ExtensionSettings>) : {}) };
  const clamp = (value: number, min: number, max: number, fallback: number) => (Number.isFinite(value) ? Math.min(max, Math.max(min, value)) : fallback);
  return {
    ...s,
    fontSize: Math.round(clamp(s.fontSize, 0, FONT_SCALES.length - 1, DEFAULT_SETTINGS.fontSize)),
    backgroundOpacity: clamp(s.backgroundOpacity, 0, 100, DEFAULT_SETTINGS.backgroundOpacity),
    position: clamp(s.position, POSITION_RANGE.min, POSITION_RANGE.max, DEFAULT_SETTINGS.position),
    hideMode: (['off', 'translation', 'subtitle', 'translation-then-subtitle', 'subtitle-then-translation'] as const).includes(s.hideMode) ? s.hideMode : 'off',
    font: (['default', 'sans', 'serif'] as const).includes(s.font) ? s.font : 'default',
  };
}

/** Which of the line and its translation are still hidden after [revealed] presses of E (or hovers). */
export function hidden(mode: HideMode, revealed: number): { line: boolean; translation: boolean } {
  switch (mode) {
    case 'off':
      return { line: false, translation: false };
    case 'translation':
      return { line: false, translation: revealed < 1 };
    case 'subtitle':
      return { line: revealed < 1, translation: false };
    case 'translation-then-subtitle':
      return { line: revealed < 2, translation: revealed < 1 };
    case 'subtitle-then-translation':
      return { line: revealed < 1, translation: revealed < 2 };
  }
}

export async function loadSettings(): Promise<ExtensionSettings> {
  const saved = await browser.storage.local.get('settings');
  return normalize(saved.settings);
}

export async function saveSettings(settings: ExtensionSettings): Promise<void> {
  await browser.storage.local.set({ settings });
}
