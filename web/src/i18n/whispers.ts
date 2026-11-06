// Tip.
import type { Language } from "../game/types";

// Bölümde ölüm cümlesi yoksa akıl düşükken kullanılan genel cümleler.
export const HALLUCINATIONS: Record<Language, string[]> = {
  en: [
    "You’re going to die.",
    "It’s all your fault.",
    "Nobody gets out.",
    "Midnight is coming.",
    "You already know how this ends.",
    "Look up.",
  ],
  tr: [
    "Öleceksin.",
    "Hepsi senin yüzünden.",
    "Kimse çıkamayacak.",
    "Gece yarısı geliyor.",
    "Nasıl biteceğini biliyorsun.",
    "Yukarı bak.",
  ],
  pl: [
    "Umrzesz.",
    "To wszystko twoja wina.",
    "Nikt się nie wydostanie.",
    "Nadchodzi północ.",
    "Wiesz, jak to się skończy.",
    "Spójrz w górę.",
  ],
};
