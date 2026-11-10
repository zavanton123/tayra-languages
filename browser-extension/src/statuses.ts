/** Tayra's word statuses (TermStatus), as the bridge numbers them. */
export const UNKNOWN = 0;
export const IGNORED = 98;
export const KNOWN = 99;

/** The buttons of the word card, in the reader's order. */
export const STATUS_BUTTONS: { value: number; label: string; title: string }[] = [
  { value: 1, label: '1', title: 'Learning 1' },
  { value: 2, label: '2', title: 'Learning 2' },
  { value: 3, label: '3', title: 'Learning 3' },
  { value: 4, label: '4', title: 'Learning 4' },
  { value: KNOWN, label: '✓', title: 'Known' },
  { value: IGNORED, label: '✕', title: 'Ignore' },
];

/** The class a word of [status] is drawn with: new words blue, words being learned yellow fading as they rise, the rest plain. */
export function statusClass(status: number | null | undefined): string {
  if (status === null || status === undefined) return 'plain';
  if (status === UNKNOWN) return 's0';
  if (status >= 1 && status <= 4) return `s${status}`;
  return 'plain';
}
