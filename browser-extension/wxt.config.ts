import { defineConfig } from 'wxt';

export default defineConfig({
  manifest: ({ browser }) => ({
    name: 'Tayra for YouTube',
    description: 'Watch YouTube with your Tayra words: coloured subtitles, translations and lookups from the Tayra app.',
    permissions: ['storage'],
    host_permissions: ['https://www.youtube.com/*', 'http://127.0.0.1/*'],
    ...(browser === 'firefox' ? { browser_specific_settings: { gecko: { id: 'youtube@tayra.app', strict_min_version: '128.0' } } } : {}),
  }),
});
