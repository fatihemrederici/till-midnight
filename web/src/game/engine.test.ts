// Test araçları.
import { describe, expect, it } from "vitest";
// Saat hesapları.
import { dayProgress } from "./clock";
// Test edilen motor.
import { cardText, choose, holds, newGame } from "./engine";
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
