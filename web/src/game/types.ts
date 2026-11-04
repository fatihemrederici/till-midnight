// Bu tipler Java'daki Story record'unun TypeScript karşılığı. Swift'teki struct tanımları gibi.

// Oyunun dilleri.
export type Language = "en" | "tr" | "pl";

// Üç dilde bir metin.
export type Text = Record<Language, string>;

// Koşul: {"elif": true} işaret doğru olmalı demektir.
export type Condition = Record<string, boolean>;

// Kaydırma yönü.
export type Side = "left" | "right";

// Kartı kaydırmanın sonucu.
export type Choice = {
  label: Text;
  effects?: Record<string, number>;
  set?: Condition;
  death?: { when?: Condition; ending: string };
  after?: Text;
};

// Bir kart.
export type Card = {
  id: string;
  time: string;
  emoji: string;
  who: Text;
  text: Text;
  variants?: { when: Condition; text: Text }[];
  when?: Condition;
  left: Choice;
  right: Choice;
};

// Bir gösterge.
export type Meter = { id: string; emoji: string; label: Text; ending: string };

// Bir son.
export type Ending = {
  id: string;
  won: boolean;
  when?: Condition;
  title: Text;
  text: Text;
  chain: { when?: Condition; text: Text }[];
  hint: Text;
};

// Hikâyenin tamamı.
export type Story = {
  formatVersion: number;
  id: string;
  title: Text;
  vision: Text[];
  wake: Text;
  start: Record<string, number>;
  maxMeter: number;
  meters: Meter[];
  cards: Card[];
  endings: Ending[];
};

// Oyunun anlık hali. endingId doluysa oyun bitti.
export type GameState = {
  flags: Condition;
  meters: Record<string, number>;
  index: number;
  endingId: string | null;
  // Son seçimin sonucu olan cümle. Bir sonraki kartın üstünde görünür.
  after: Text | null;
  // Bu koşudaki seçimler. Sunucu bunları baştan oynatıp sonucu doğrular.
  choices: Side[];
};

// Sunucunun koşu cevabındaki istatistikler.
export type RunStats = { runs: number; players: number; survived: number; sameEnding: number };
