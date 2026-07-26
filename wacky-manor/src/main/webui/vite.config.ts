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
        target: 'ws://localhost:8080',
        ws: true,
      },
      '/manor': {
        target: 'http://localhost:8080',
      },
    },
  },
});
