import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api'],
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '^/api/': 'http://localhost:8080',
      '/ws': { target: 'http://localhost:8080', ws: true },
      '/allure-report': 'http://localhost:8080',
      '/dev-api': {
        target: 'https://192.168.6.171:8088',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
