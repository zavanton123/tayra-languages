import { DEFAULT_DICTIONARIES, validDictionaries, type DictionaryLink } from './dictionaries';
import { WORD_GROUPS, type WordGroup, type WordOrder } from './words';

export type PanelTab = 'subtitles' | 'words' | 'saved';

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
  /** The panel of subtitles and words beside the video, in YouTube's right column. */
  sidePanel: boolean;
  /** Whether the side panel is open or folded to its tabs. */
  panelOpen: boolean;
  panelTab: PanelTab;
  /** The side panel's lines show their translation always, rather than on hover. */
  panelTranslations: boolean;
  wordsOrder: WordOrder;
  /** The groups of words the Words tab lists. */
  wordsShown: WordGroup[];
  /** A small card with a word's meaning while the pointer rests on it. */
  hoverLookup: boolean;
  /** Clicking a word says it too. */
  speakOnClick: boolean;
  /** The websites a word's card links to. */
  dictionaries: DictionaryLink[];
  /** The dictionary Shift+click opens: the one used last. */
  lastDictionary: string | null;
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
  sidePanel: true,
  panelOpen: true,
  panelTab: 'subtitles',
  panelTranslations: false,
  wordsOrder: 'rank',
  wordsShown: ['new', 'learning'],
  hoverLookup: true,
  speakOnClick: false,
  dictionaries: DEFAULT_DICTIONARIES,
  lastDictionary: null,
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
    panelTab: (['subtitles', 'words', 'saved'] as const).includes(s.panelTab) ? s.panelTab : 'subtitles',
    wordsOrder: (['video', 'rank', 'count'] as const).includes(s.wordsOrder) ? s.wordsOrder : 'rank',
    wordsShown: Array.isArray(s.wordsShown) ? WORD_GROUPS.filter((g) => s.wordsShown.includes(g)) : DEFAULT_SETTINGS.wordsShown,
    dictionaries: validDictionaries(s.dictionaries),
    lastDictionary: typeof s.lastDictionary === 'string' ? s.lastDictionary : null,
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
