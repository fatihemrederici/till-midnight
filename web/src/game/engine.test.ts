// Test araçları.
import { describe, expect, it } from "vitest";
// Saat hesapları.
import { dayProgress } from "./clock";
// Test edilen motor.
import { canAfford, cardText, choose, currentCard, holds, newGame } from "./engine";
// Tipler.
import type { Card, Side, Story, Text } from "./types";

// Tüm bölümlerin kuralları. Motor metinlere bakmadığı için dil dosyaları gerekmiyor.
const stories = import.meta.glob("../../../stories/*/story.json", { eager: true });
// Beklenen yollar: Java testinin kullandığı dosyalar. Satırlar "LRL stairs" biçiminde.
const allPaths = import.meta.glob("../../../api/src/test/resources/story-paths/*.txt", {
  eager: true,
  query: "?raw",
  import: "default",
}) as Record<string, string>;
// Bölüm kimliği -> kurallar.
const chapters = Object.values(stories).map((module) => module as unknown as Story);

// Sıradaki kartın kimliği.
const story_card = (story: Story, index: number) => story.cards[index].id;

// Testler için üç dilde aynı metin.
const text = (value: string): Text => ({ en: value, tr: value, pl: value });

// Motorun testleri.
describe("engine", () => {
  // Yeni bölüm eklenince test otomatik onu da kapsar.
  it("finds every chapter", () => {
    // Üç bölüm var.
    expect(chapters.map((story) => story.id).sort()).toEqual([
      "first-day",
      "last-dance",
      "night-shift",
    ]);
  });

  // Java motoruyla birebir aynı sonuçlar çıkmalı.
  it.each(chapters.map((story) => [story.id, story] as const))(
    "ends every known path of %s like the server does",
    (id, story) => {
      // Bölümün yol dosyası.
      const path = Object.keys(allPaths).find((key) => key.endsWith(`/${id}.txt`))!;
      // Dosyanın satırları.
      const lines = allPaths[path].trim().split("\n");
      // Her yolu dene.
      for (const line of lines) {
        // Seçimleri ve sonu ayır.
        const [choices, ending] = line.split(" ");
        // Oyunu başlat.
        let state = newGame(story);
        // Her seçimi uygula.
        for (const letter of choices) {
          const side: Side = letter === "L" ? "left" : "right";
          state = choose(story, state, side);
        }
        // Beklenen son.
        expect(state.endingId).toBe(ending);
        // Seçimler kaydedilmiş olmalı.
        expect(state.choices.length).toBe(choices.length);
      }
    },
  );

  // Sıfıra inen gösterge krize girmeli, ödenince 1'e dönmeli.
  it("opens a crisis card when a meter runs out", () => {
    // En küçük hikâye.
    const free = { label: text("l"), after: text("a") };
    const mini = {
      start: { m: 1, o: 5 },
      maxMeter: 5,
      meters: [
        {
          id: "m",
          ending: "gone",
          crisis: {
            id: "c",
            left: { ...free, effects: { o: -2 }, requires: { o: 2 } },
            right: free,
          },
        },
        { id: "o", ending: "other" },
      ],
      cards: [
        { id: "a", time: "10:00", left: { ...free, effects: { m: -1 } }, right: free },
        { id: "b", time: "11:00", left: free, right: free },
      ],
      endings: [{ id: "win", won: true }],
    } as unknown as Story;
    // Göstergeyi sıfıra indir.
    let state = choose(mini, newGame(mini), "left");
    expect(state.crisis).toBe("m");
    expect(state.endingId).toBeNull();
    // Kriz kartı açık, ödeyebiliyoruz.
    expect(currentCard(mini, state).id).toBe("c");
    expect(canAfford(currentCard(mini, state).left, state.meters)).toBe(true);
    // Öde.
    state = choose(mini, state, "left");
    expect(state.meters).toEqual({ m: 1, o: 3 });
    expect(state.crisis).toBeNull();
    // Saat son oynanan karttan okunur, sıradaki kart gelir.
    expect(state.lastIndex).toBe(0);
    expect(story_card(mini, state.index)).toBe("b");
  });

  // Ayarlanmamış işaret false sayılmalı.
  it("treats missing flags as false", () => {
    // Beklenti false, işaret yok: tutar.
    expect(holds({ elif: false }, {})).toBe(true);
    // Beklenti true, işaret yok: tutmaz.
    expect(holds({ elif: true }, {})).toBe(false);
  });

  // Durum değişken metni seçmeli.
  it("picks the variant text when its condition holds", () => {
    // En küçük kart.
    const card = {
      text: text("normal"),
      variants: [{ when: { warned: true }, text: text("warned") }],
    } as unknown as Card;
    // Koşul tutuyorsa değişken.
    expect(cardText(card, { warned: true }).en).toBe("warned");
    // Tutmuyorsa normal metin.
    expect(cardText(card, {}).en).toBe("normal");
  });

  // Saat, gün çubuğuna doğru çevrilmeli.
  it("converts the clock to day progress", () => {
    // Bölümün başlangıcı.
    expect(dayProgress("08:00", "08:00")).toBe(0);
    // Sabah başlayan bölümde 16:00 yolun yarısı.
    expect(dayProgress("16:00", "08:00")).toBe(0.5);
    // Akşam başlayan bölümde 22:00 da yolun yarısı.
    expect(dayProgress("22:00", "20:00")).toBe(0.5);
  });
});
