// Hikâye motoru. Java'daki StoryEngine'in birebir kopyası: Tarayıcı anında cevap versin diye burada da var.
// İki motor aynı test dosyasıyla (story-paths) kontrol edilir; biri değişip öbürü değişmezse test kırılır.
import type { Card, ChainStep, Choice, Condition, GameState, Side, Story, Text } from "./types";

// Koşul tutuyor mu? Hiç ayarlanmamış işaret false sayılır. Koşul yoksa true.
export function holds(condition: Condition | undefined, flags: Condition): boolean {
  // Koşul yok.
  if (!condition) return true;
  // Her işaret beklenen değerde mi?
  return Object.entries(condition).every(([flag, value]) => (flags[flag] ?? false) === value);
}

// from sırasından başlayarak koşulu tutan ilk kartı bulur. Yoksa -1.
function nextCard(story: Story, flags: Condition, from: number): number {
  // Kartları dön.
  for (let i = from; i < story.cards.length; i++) {
    if (holds(story.cards[i].when, flags)) return i;
  }
  // Kart kalmadı.
  return -1;
}

// Yeni bir oyun başlatır.
export function newGame(story: Story): GameState {
  return {
    flags: {},
    meters: { ...story.start },
    index: nextCard(story, {}, 0),
    endingId: null,
    after: null,
    choices: [],
  };
}

// Oyuncunun önündeki kart.
export function currentCard(story: Story, state: GameState): Card {
  return story.cards[state.index];
}

// Seçimin bu durumda tetiklediği ölüm. Yoksa undefined.
function deathOf(choice: Choice, flags: Condition) {
  // İlk tutan ölüm kazanır.
  return choice.deaths?.find((death) => holds(death.when, flags));
}

// Bir seçimi uygular ve yeni durumu döndürür. Eski durum değişmez.
export function choose(story: Story, state: GameState, side: Side): GameState {
  // Şu anki kart ve seçim.
  const card = currentCard(story, state);
  const choice = card[side];
  // Seçimler listesine ekle.
  const choices = [...state.choices, side];

  // Ölüm koşulu, seçimin işaretleri eklenmeden önceki duruma bakar.
  const death = deathOf(choice, state.flags);
  if (death) return { ...state, choices, endingId: death.ending, after: null };

  // İşaretler ve göstergeler.
  const flags = { ...state.flags, ...choice.set };
  const meters = { ...state.meters };
  for (const [meter, change] of Object.entries(choice.effects ?? {})) {
    // 0 ile en yüksek değer arasında tut.
    meters[meter] = Math.min(story.maxMeter, Math.max(0, meters[meter] + change));
  }

  // Sıfıra inen ilk gösterge, bu kartın yerine göre oyunu bitirir.
  const empty = story.meters.find((meter) => meters[meter.id] <= 0);
  if (empty) {
    return {
      flags,
      meters,
      index: state.index,
      choices,
      endingId: empty.endings[card.place],
      after: null,
    };
  }
  // Sıradaki kart. Yoksa gece yarısına ulaşıldı.
  const index = nextCard(story, flags, state.index + 1);
  if (index < 0) {
    const win = story.endings.find((ending) => ending.won && holds(ending.when, flags));
    return { flags, meters, index, choices, endingId: win ? win.id : null, after: null };
  }
  // Oyun sürüyor.
  return { flags, meters, index, choices, endingId: null, after: choice.after ?? null };
}

// Kartın o anki metni: Koşulu tutan ilk varyant, yoksa normal metin.
export function cardText(card: Card, flags: Condition): Text {
  const variant = card.variants?.find((item) => holds(item.when, flags));
  return variant ? variant.text : card.text;
}

// Bir seçimin bu koşudaki tek adımı.
type Step = { card: Card; choice: Choice; flags: Condition };

// Gösterge ölümünde zincir bu kadar adım gösterir.
const METER_CHAIN = 4;

// Ölüm ekranındaki zincir. Koşuyu baştan oynatır ve ölüme götüren seçimleri saatleriyle bulur.
// Tuzak ölümü: Ölümün koşulundaki işaretleri kuran seçimler, onların kartlarının koşullarını kuranlar ve son seçim.
// Gösterge ölümü: O göstergeyi düşüren son seçimler.
export function deathChain(story: Story, choices: Side[]): ChainStep[] {
  // Koşuyu baştan oynat, her adımı kaydet.
  const steps: Step[] = [];
  let state = newGame(story);
  for (const side of choices) {
    const card = currentCard(story, state);
    steps.push({ card, choice: card[side], flags: state.flags });
    state = choose(story, state, side);
  }
  // Ölüm yoksa zincir yok.
  const ending = story.endings.find((item) => item.id === state.endingId);
  const last = steps[steps.length - 1];
  if (!ending || ending.won || !last) return [];
  // Adımı zincir satırına çevirir.
  const line = (step: Step): ChainStep => ({ time: step.card.time, text: step.choice.past });

  // Gösterge ölümü mü?
  const meter = story.meters.find((item) => Object.values(item.endings).includes(ending.id));
  if (meter) {
    // Göstergeyi düşüren seçimler, sondan.
    const lowering = steps.filter((step) => (step.choice.effects?.[meter.id] ?? 0) < 0);
    return lowering.slice(-METER_CHAIN).map(line);
  }

  // Tuzak ölümü: Gereken işaretler, öldüren kuraldan ve kartın koşulundan.
  const rule = deathOf(last.choice, last.flags);
  const seen = new Set<string>();
  const queue: string[] = [];
  // Koşuldaki doğru işaretleri sıraya ekler.
  const need = (condition?: Condition) =>
    Object.entries(condition ?? {}).forEach(([flag, value]) => {
      if (value && !seen.has(flag)) {
        seen.add(flag);
        queue.push(flag);
      }
    });
  need(rule?.when);
  need(last.card.when);
  // Zincire giren adımların sıra numaraları.
  const picked = new Set<number>();
  // Sıradaki işaret kalmayana kadar.
  while (queue.length > 0) {
    const flag = queue.pop()!;
    // Bu işareti en son doğru yapan adım.
    for (let i = steps.length - 2; i >= 0; i--) {
      if (steps[i].choice.set?.[flag] !== true) continue;
      if (!picked.has(i)) {
        picked.add(i);
        // O kart da bir koşula bağlıysa, onun sebebi de zincire girer.
        need(steps[i].card.when);
      }
      break;
    }
  }
  // Saat sırasıyla zincir, en sonda öldüren seçim.
  return [...picked]
    .sort((a, b) => a - b)
    .map((i) => line(steps[i]))
    .concat(line(last));
}
