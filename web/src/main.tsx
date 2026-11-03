// Geliştirme sırasında olası hataları yakalamaya yardım eden React bileşeni.
import { StrictMode } from "react";
// React'i HTML sayfasına bağlayan fonksiyon.
import { createRoot } from "react-dom/client";
// Ana bileşenimiz.
import { App } from "./App";
// Stiller.
import "./App.css";

// index.html içindeki root kutusunu bul ve uygulamayı oraya çiz.
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);