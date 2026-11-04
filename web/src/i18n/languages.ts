// Depo.
import { readValue, writeValue } from "../data/storage";
// Tipler.
import type { Language, Text } from "../game/types";

// Desteklenen diller ve seçicide görünen adları.
export const LANGUAGES: { id: Language; label: string }[] = [
  { id: "en", label: "EN" },
  { id: "tr", label: "TR" },
  { id: "pl", label: "PL" },
];

// Değer bizim dillerimizden biri mi?
function isLanguage(value: string | null): value is Language {
  // Listede ara.
  return LANGUAGES.some((language) => language.id === value);
}

// Açılışta kullanılacak dil: Önce seçilmiş dil, sonra telefonun dili, yoksa İngilizce.
export function initialLanguage(): Language {
  // Saklı dil.
  const saved = readValue("language");
  if (isLanguage(saved)) return saved;
  // Telefonun dilinin ilk iki harfi.
  const device = (navigator.language || "en").slice(0, 2);
  // Destekleniyorsa onu seç.
  return isLanguage(device) ? device : "en";
}

// Seçilen dili saklar.
export function saveLanguage(language: Language): void {
  // Depoya yaz.
  writeValue("language", language);
}

// Bir metnin seçili dildeki hali.
export function say(text: Text, language: Language): string {
  // Metni döndür.
  return text[language];
}
