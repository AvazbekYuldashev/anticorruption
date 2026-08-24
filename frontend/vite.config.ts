import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    /*
     * Dev rejimida barcha /api so'rovlari backend'ga uzatiladi. Shu tufayli
     * brauzer nuqtai nazaridan frontend va API bitta manzilda bo'ladi -
     * CORS umuman ishtirok etmaydi va ishlab chiqish sozlamasi soddalashadi.
     */
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
