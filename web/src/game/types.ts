// Bu tipler Java'daki Story record'unun TypeScript karşılığı. Swift'teki struct tanımları gibi.

// Oyunun dilleri.
export type Language = "en" | "tr" | "pl";

// Üç dilde bir metin.
export type Text = Record<Language, string>;

// Koşul: {"emily": true} işaret doğru olmalı demektir.
export type Condition = Record<string, boolean>;

// Kaydırma yönü.
export type Side = "left" | "right";

// Kartın geçtiği yer. Gösterge ölümü bu yere göre seçilir.
export type Place = "street" | "home" | "work";

// Bir seçimin ölüm kuralı: Koşul tutarsa oyun biter. Koşul yoksa her zaman öldürür.
export type Death = { when?: Condition; ending: string };

// Kartı kaydırmanın sonucu.
export type Choice = {
  label: Text;
  effects?: Record<string, number>;
  set?: Condition;
  // Koşulu tutan ilk ölüm kazanır.
  deaths?: Death[];
  after?: Text;
  // Önceki seçimlere göre after yerine geçen metinler. İlk tutan kazanır.
  variants?: { when: Condition; text: Text }[];
  // Ölüm zincirinde bu seçimi anlatan cümle.
  past: Text;
};

// Bir kart.
export type Card = {
  id: string;
  time: string;
  // Ekranda gösterilen saat, gerçek saatten farklıysa. Örneğin ileri giden mutfak saati.
  shownTime?: string;
  place: Place;
  emoji: string;
  // Kartın fotoğrafı: Bölümün images klasöründeki dosya. Yoksa emoji görünür.
  image?: string;
  who: Text;
  text: Text;
  variants?: { when: Condition; text: Text }[];
  when?: Condition;
  left: Choice;
  right: Choice;
};

// Bir gösterge: Her bölümde akıl ve sağlık. Sıfıra inince yere göre bir ölüm gelir.
export type Meter = {
  id: "mind" | "body";
  emoji: string;
  label: Text;
  endings: Record<Place, string>;
};

// Bir son.
export type Ending = {
  id: string;
  won: boolean;
  when?: Condition;
  title: Text;
  text: Text;
  hint: Text;
  // Akıl düşükken kartın yerine bir anlığına geçen, bu ölümü ima eden cümle.
  omen?: Text;
};

// Bölüm seçme ekranındaki bir satır.
export type StorySummary = { id: string; title: Text; deaths: string[] };

// Hikâyenin tamamı.
export type Story = {
  formatVersion: number;
  id: string;
  // Kuralların parmak izi. Sunucu, koşunun hangi kurallarla oynandığını bundan anlar.
  version: string;
  title: Text;
  vision: Text[];
  wake: Text;
  start: Record<string, number>;
  maxMeter: number;
  meters: Meter[];
  // Gizli tuzak işaretleri. Kurulunca sadece hafif bir ses çalar.
  traps: string[];
  cards: Card[];
  endings: Ending[];
  // Oyuncunun bulabileceği ölümler. Sunucu hesaplar.
  deaths: string[];
};

// Oyunun anlık hali. endingId doluysa oyun bitti.
export type GameState = {
  flags: Condition;
  meters: Record<string, number>;
  // Sıradaki kart. Oyun ölümle bittiyse öldüğün kart.
  index: number;
  endingId: string | null;
  // Son seçimin sonucu olan cümle. Bir sonraki kartın üstünde görünür.
  after: Text | null;
  // Bu koşudaki seçimler. Sunucu bunları baştan oynatıp sonucu doğrular.
  choices: Side[];
};

// Ölüm zincirinin bir adımı: Saat ve o seçimin geçmiş zaman cümlesi.
export type ChainStep = { time: string; text: Text };

// Sunucunun koşu cevabındaki istatistikler.
export type RunStats = { runs: number; players: number; survived: number; sameEnding: number };
