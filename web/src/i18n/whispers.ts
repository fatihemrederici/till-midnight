// Tip.
import type { Language } from "../game/types";

// Güven bitince ekranın kenarında beliren fısıltılar.
export const WHISPERS: Record<Language, string[]> = {
  en: [
    "Is he crazy?",
    "Did you hear?",
    "Not again.",
    "Why is he still here?",
    "Nobody believes him.",
    "Don’t look at him.",
  ],
  tr: [
    "Deli bu.",
    "Duydun mu?",
    "Yine başladı.",
    "Bu hâlâ burada mı?",
    "Kimse inanmıyor.",
    "Bakma ona.",
  ],
  pl: [
    "Czy on oszalał?",
    "Słyszałeś?",
    "Znowu to samo.",
    "Czemu on tu jeszcze jest?",
    "Nikt mu nie wierzy.",
    "Nie patrz na niego.",
  ],
};
