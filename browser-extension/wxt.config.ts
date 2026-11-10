import { defineConfig } from 'wxt';

export default defineConfig({
  manifest: {
    name: 'Tayra for YouTube',
    description: 'Watch YouTube with your Tayra words: coloured subtitles, translations and lookups from the Tayra app.',
    permissions: ['storage'],
    host_permissions: ['https://www.youtube.com/*'],
    browser_specific_settings: {
      gecko: { id: 'youtube@tayra.app', strict_min_version: '128.0' },
    },
  },
});
