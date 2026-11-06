// Efekt ayarı: "Tam" ya da "Az". Az seçilince göz kapakları, kalp atışı ve parazit kapanır, sesler kalır.
import { readValue, writeValue } from "./storage";

// Kayıt anahtarı.
const KEY = "effects";

// Efektler azaltılmış mı?
export function isLowEffects(): boolean {
  return readValue(KEY) === "low";
}

// Ayarı sayfaya uygular: CSS, html etiketindeki data-effects değerine bakar.
export function applyEffects(): void {
  document.documentElement.dataset.effects = isLowEffects() ? "low" : "full";
}

// Ayarı değiştirir ve uygular.
export function setLowEffects(low: boolean): void {
  writeValue(KEY, low ? "low" : "full");
  applyEffects();
}
