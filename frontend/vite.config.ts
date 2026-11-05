import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // Em desenvolvimento, o Vite reencaminha /api para o backend (DA02, DA34).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    // Para entregar, o build vai para os recursos estáticos do backend (DA28).
    outDir: fileURLToPath(new URL('../backend/src/main/resources/static', import.meta.url)),
    emptyOutDir: true,
  },
})
