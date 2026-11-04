// Tipler.
import type { Card, Condition, GameState, Side, Story, Text, Language } from "./types";

// Kural motoru. Java'daki StoryEngine'in kopyası: Kaydırma anında tepki versin diye tarayıcıda da çalışıyor.
// İkisi de stories/first-day.paths.json ile test edildiği için birbirinden ayrılamaz.

// Koşul tutuyor mu? Ayarlanmamış işaret false sayılır. Koşul yoksa true.
export function holds(condition: Condition | undefined, flags: Condition): boolean {
  // Koşul yoksa her zaman tutar.
  if (!condition) return true;
  // Her koşul işaretle aynı olmalı.
  return Object.entries(condition).every(([flag, value]) => (flags[flag] ?? false) === value);
}

// from sırasından başlayarak koşulu tutan ilk kartı bulur. Yoksa -1.
function nextCard(story: Story, flags: Condition, from: number): number {
  // Kartları dön.
  for (let i = from; i < story.cards.length; i++) {
    // Koşul tutuyorsa bu karttır.
    if (holds(story.cards[i].when, flags)) return i;
  }
  // Kart kalmadı.
  return -1;
}

// Yeni oyun başlatır.
export function newGame(story: Story): GameState {
  // Hiç işaret yok, başlangıç göstergeleri, ilk uygun kart.
  return { flags: {}, meters: { ...story.start }, index: nextCard(story, {}, 0), endingId: null, after: null, choices: [] };
}

// Seçimi uygular ve yeni durumu döndürür. Eski durum değişmez.
export function choose(story: Story, state: GameState, side: Side): GameState {
  // Şu anki kart ve seçilen taraf.
  const choice = story.cards[state.index][side];
  // Seçimi kayda ekle.
  const choices = [...state.choices, side];

  // Ölüm koşulu, seçimin işaretleri eklenmeden önceki duruma bakar.
  if (choice.death && holds(choice.death.when, state.flags)) {
    return { ...state, choices, endingId: choice.death.ending, after: null };
  }

  // Seçimin işaretlerini ekle.
  const flags = { ...state.flags, ...choice.set };
  // Göstergeleri kopyala.
  const meters = { ...state.meters };
  // Etkileri uygula, 0 ile en yüksek değer arasında tut.
  for (const [meter, change] of Object.entries(choice.effects ?? {})) {
    meters[meter] = Math.min(story.maxMeter, Math.max(0, meters[meter] + change));
  }

  // Sıfıra inen gösterge varsa dosyadaki sırayla ilki oyunu bitirir.
  const empty = story.meters.find((meter) => meters[meter.id] <= 0);
  if (empty) return { flags, meters, index: state.index, choices, endingId: empty.ending, after: null };

  // Sıradaki kartı bul.
  const index = nextCard(story, flags, state.index + 1);
  // Kart kalmadıysa gece yarısına ulaşıldı: koşulu tutan ilk kurtuluş.
  if (index < 0) {
    const win = story.endings.find((ending) => ending.won && holds(ending.when, flags));
    return { flags, meters, index: state.index, choices, endingId: win ? win.id : null, after: null };
  }
  // Oyun sürüyor.
  return { flags, meters, index, choices, endingId: null, after: choice.after ?? null };
}

// Kartın metni: Koşulu tutan değişken varsa o, yoksa normal metin.
export function cardText(card: Card, flags: Condition): Text {
  // Değişkenlere bak.
  const variant = card.variants?.find((item) => holds(item.when, flags));
  // Bulunan metni döndür.
  return variant ? variant.text : card.text;
}

// Bir metnin seçili dildeki hali.
export function say(text: Text, language: Language): string {
  // Metni döndür.
  return text[language];
}

// "14:30" saatini günün ilerleme oranına çevirir: 08:00 başlangıç, 24:00 son.
export function dayProgress(time: string): number {
  // Saat ve dakikayı ayır.
  const [hours, minutes] = time.split(":").map(Number);
  // 0 ile 1 arasına çevir.
  return Math.max(0, Math.min(1, (hours + minutes / 60 - 8) / 16));
}
