// React'i HTML sayfasına bağlayan fonksiyon.
import { createRoot } from "react-dom/client";
// Efekt ayarı.
import { applyEffects } from "./data/effects";
// Ana bileşenimiz.
import { App } from "./App";
// Stiller.
import "./styles/index.css";

// index.html içindeki root kutusunu bul ve uygulamayı oraya çiz.
// StrictMode yok: Geliştirme sırasında efektleri iki kez çalıştırıyor, bu da her sesi iki kez çaldırırdı.
// Efekt ayarını ilk çizimden önce uygula.
applyEffects();
createRoot(document.getElementById("root")!).render(<App />);
