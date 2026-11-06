// Tipler.
import type { Card, Choice, Condition, GameState, Side, Story, Text } from "./types";

// Kural motoru. Java'daki StoryEngine'in kopyası: Kaydırma anında tepki versin diye tarayıcıda da çalışıyor.
// İkisi de aynı yol dosyalarıyla test edildiği için birbirinden ayrılamaz.

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
  const index = nextCard(story, {}, 0);
  return {
    flags: {},
    meters: { ...story.start },
    index,
    endingId: null,
    after: null,
    choices: [],
    crisis: null,
    usedCrises: [],
    lastIndex: index,
  };
}

// Oyuncunun önündeki kart: Kriz varsa kriz kartı, yoksa sıradaki kart.
export function currentCard(story: Story, state: GameState): Omit<Card, "time"> {
  // Kriz yoksa normal kart.
  if (!state.crisis) return story.cards[state.index];
  // Krizdeki göstergenin kartı.
  return story.meters.find((meter) => meter.id === state.crisis)!.crisis!;
}

// Göstergeler bu seçim için yetiyor mu?
export function canAfford(choice: Choice, meters: Record<string, number>): boolean {
  // Her şartı kontrol et.
  return Object.entries(choice.requires ?? {}).every(([meter, need]) => meters[meter] >= need);
}

// Göstergelere bakıp oyunun nasıl süreceğine karar verir.
function settle(
  story: Story,
  flags: Condition,
  meters: Record<string, number>,
  next: number,
  usedCrises: string[],
  choices: Side[],
  after: Text | null,
  lastIndex: number,
): GameState {
  // Ortak alanlar.
  const base = { flags, meters, index: next, choices, usedCrises, lastIndex };
  // Sıfıra inen ilk gösterge.
  const empty = story.meters.find((meter) => meters[meter.id] <= 0);
  if (empty) {
    // Kullanılmamış ve ödenebilir bir krizi varsa oyuncuya şans verilir.
    const crisis = empty.crisis;
    const payable =
      crisis &&
      !usedCrises.includes(empty.id) &&
      (canAfford(crisis.left, meters) || canAfford(crisis.right, meters));
    if (payable) return { ...base, endingId: null, after, crisis: empty.id };
    // Yoksa bu göstergenin sonu gelir.
    return { ...base, endingId: empty.ending, after: null, crisis: null };
  }
  // Kart kalmadıysa gece yarısına ulaşıldı: Koşulu tutan ilk kurtuluş.
  if (next < 0) {
    const win = story.endings.find((ending) => ending.won && holds(ending.when, flags));
    return { ...base, endingId: win ? win.id : null, after: null, crisis: null };
  }
  // Oyun sürüyor.
  return { ...base, endingId: null, after, crisis: null };
}

// Seçimi uygular ve yeni durumu döndürür. Eski durum değişmez.
export function choose(story: Story, state: GameState, side: Side): GameState {
  // Seçilen taraf.
  const choice = currentCard(story, state)[side];
  // Seçimi kayda ekle.
  const choices = [...state.choices, side];
  // Ölüm koşulu, seçimin işaretleri eklenmeden önceki duruma bakar.
  if (choice.death && holds(choice.death.when, state.flags)) {
    return { ...state, choices, endingId: choice.death.ending, after: null, crisis: null };
  }

  // Seçimin işaretlerini ekle.
  const flags = { ...state.flags, ...choice.set };
  // Göstergeleri kopyala.
  const meters = { ...state.meters };
  // Etkileri uygula, 0 ile en yüksek değer arasında tut.
  for (const [meter, change] of Object.entries(choice.effects ?? {})) {
    meters[meter] = Math.min(story.maxMeter, Math.max(0, meters[meter] + change));
  }
  // Sonuç cümlesi.
  const after = choice.after ?? null;

  // Kriz atlatıldıysa gösterge 1'e döner ve bu kriz bir daha çıkmaz.
  if (state.crisis) {
    meters[state.crisis] = 1;
    const used = [...state.usedCrises, state.crisis];
    return settle(story, flags, meters, state.index, used, choices, after, state.lastIndex);
  }
  // Normal kartta sıra bir sonraki uygun karta geçer.
  const next = nextCard(story, flags, state.index + 1);
  return settle(story, flags, meters, next, state.usedCrises, choices, after, state.index);
}

// Kartın metni: Koşulu tutan değişken varsa o, yoksa normal metin.
export function cardText(card: Omit<Card, "time">, flags: Condition): Text {
  // Değişkenlere bak.
  const variant = card.variants?.find((item) => holds(item.when, flags));
  // Bulunan metni döndür.
  return variant ? variant.text : card.text;
}
