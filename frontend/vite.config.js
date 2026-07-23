import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy manda todo lo que empiece con /api al backend Spring Boot (localhost:8080).
// Así el frontend llama a rutas relativas (/api/...) y no hay problemas de CORS en dev.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
