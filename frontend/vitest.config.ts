import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

/**
 * Testlar sozlamasi.
 *
 * <p>`vite.config.ts` dan alohida: u yerda dev-server proksi va Tailwind
 * bor, testlarga esa ular kerak emas va faqat ishga tushishni sekinlashtirardi.
 */
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    restoreMocks: true,
  },
});
