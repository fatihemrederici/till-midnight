// Vite'ın ayar fonksiyonu.
import { defineConfig } from "vite";
// React desteği.
import react from "@vitejs/plugin-react";

// Vite ayarları.
export default defineConfig({
  // React'i etkinleştir.
  plugins: [react()],
  server: {
    // Bu adreslere gelen istekleri Java sunucusuna ilet. Böylece tarayıcı tek adresle konuşur.
    proxy: {
      "/stories": "http://localhost:8080",
      "/runs": "http://localhost:8080",
    },
  },
});
