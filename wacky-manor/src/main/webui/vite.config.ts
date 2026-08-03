import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    outDir: 'dist',
    rollupOptions: {
      input: 'index.html',
    },
  },
  server: {
    proxy: {
      '/ws/manor': {
        target: 'ws://localhost:8180',
        ws: true,
      },
      '/manor': {
        target: 'http://localhost:8180',
      },
    },
  },
});
