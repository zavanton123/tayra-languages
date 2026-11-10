import { defineConfig } from 'wxt';

const CHROME_DEVELOPMENT_KEY =
  'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqfY15qWthsvCdY0ROOqVu0WL9IuoaJYU7Uuh6E+HBBY505HB6meXO3G4KlhBmreKbyW7A2LwHqy2yU0oO1Yrd1zzIsa+7g5CijisKjoyRbjQK0oOD2PCWsfJNBgFsQhrmvhe7Sv6ihCNmd2csGdIREoZm7lqalVoExVfBbGBa5HlFF5LqeqNhW4EE4jjbDwSTkQEWjoMOdTt+AW6Kgi0htaSWlyFWHnz85kiibRN/Ngue8qEfGfFIZl22n6kzEvcuMO/Oirz9Uuc2lRElQMpFK/ib90wBnZXBfnefFOpnVIMVCiMxbsXrazm28KaUEH+JdXoq0eK2fL+LKfbVzoBHwIDAQAB';

export default defineConfig({
  manifest: ({ browser }) => ({
    name: 'Tayra for YouTube',
    description: 'Watch YouTube with your Tayra words: coloured subtitles, translations and lookups from the Tayra app.',
    permissions: ['storage', 'nativeMessaging'],
    host_permissions: ['https://www.youtube.com/*', 'http://127.0.0.1/*'],
    // Fixes the development builds' ID, which the app's helper lets in (NativeHostInstaller.CHROME_EXTENSION_IDS).
    ...(browser === 'firefox' ? {} : { key: CHROME_DEVELOPMENT_KEY }),
    ...(browser === 'firefox' ? { browser_specific_settings: { gecko: { id: 'youtube@tayra.app', strict_min_version: '128.0' } } } : {}),
  }),
});
