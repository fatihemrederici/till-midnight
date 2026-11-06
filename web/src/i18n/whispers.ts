// Tip.
import type { Language } from "../game/types";

// Güven düşünce sonuç satırında beliren fısıltılar.
export const WHISPERS: Record<Language, string[]> = {
  en: [
    "Is he crazy?",
    "Did you hear what he said?",
    "Not again.",
    "Why is he still here?",
    "Nobody believes him.",
    "Don’t look at him.",
  ],
  tr: [
    "Bu adam deli galiba.",
    "Ne dediğini duydun mu?",
    "Yine başladı.",
    "Bu hâlâ burada mı?",
    "Kimse inanmıyor ona.",
    "Bakma ona.",
  ],
  pl: [
    "Czy on oszalał?",
    "Słyszałeś, co powiedział?",
    "Znowu to samo.",
    "Czemu on tu jeszcze jest?",
    "Nikt mu nie wierzy.",
    "Nie patrz na niego.",
  ],
};

// Akıl düşünce kart metninin yerine bir anlığına geçen cümleler.
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
