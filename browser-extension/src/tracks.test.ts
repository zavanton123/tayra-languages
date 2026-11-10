import { describe, expect, it } from 'vitest';
import type { CaptionTrack } from './messages';
import { statusClass } from './statuses';
import { pickTrack, translationFor } from './tracks';

const track = (languageCode: string, kind = ''): CaptionTrack => ({ languageCode, kind, name: '', baseUrl: `https://www.youtube.com/api/timedtext?v=x&lang=${languageCode}` });

describe('pickTrack', () => {
  const tracks = [track('en'), track('pt', 'asr'), track('pt-BR'), track('es', 'asr')];
  it('takes subtitles a person made in the language being learned first', () => {
    expect(pickTrack(tracks, 'pt')?.languageCode).toBe('pt-BR');
  });
  it('takes automatic subtitles in the language when there are no others', () => {
    expect(pickTrack(tracks, 'es')).toEqual(track('es', 'asr'));
  });
  it('takes none in another language', () => {
    expect(pickTrack(tracks, 'de')).toBeNull();
  });
  it('takes the first a person made when the language is not known', () => {
    expect(pickTrack(tracks, null)?.languageCode).toBe('en');
  });
});

describe('translationFor', () => {
  it('asks for the native language unless the track is in it', () => {
    expect(translationFor(track('pt-BR'), 'en')).toBe('en');
    expect(translationFor(track('en-GB'), 'en')).toBeNull();
  });
});

describe('statusClass', () => {
  it('colours new and learning words only', () => {
    expect([0, 1, 4, 98, 99, null].map(statusClass)).toEqual(['s0', 's1', 's4', 'plain', 'plain', 'plain']);
  });
});
