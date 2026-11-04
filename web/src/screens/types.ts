// Tipler.
import type { Language, Story } from "../game/types";

// Çoğu ekranın ortak aldığı şeyler.
export type ScreenProps = {
  story: Story;
  language: Language;
  onLanguage: (language: Language) => void;
};
