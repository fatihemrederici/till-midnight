// Test araçları.
import { describe, expect, it } from "vitest";
// Bölümün kuralları. Motor metinlere bakmadığı için dil dosyaları gerekmiyor.
import rules from "../../../stories/first-day/story.json";
// Beklenen yollar: Java testinin kullandığı dosyanın aynısı. Satırlar "LRL stairs" biçiminde.
import paths from "../../../api/src/test/resources/story-paths/first-day.txt?raw";
// Saat hesapları.
import { dayProgress } from "./clock";
// Test edilen motor.
import { cardText, choose, holds, newGame } from "./engine";
// Tipler.
import type { Card, Side, Story, Text } from "./types";

// JSON'u hikâye tipi olarak kullan.
const story = rules as unknown as Story;

// Testler için üç dilde aynı metin.
const text = (value: string): Text => ({ en: value, tr: value, pl: value });

// Motorun testleri.
describe("engine", () => {
  // Java motoruyla birebir aynı sonuçlar çıkmalı.
  it("ends every known path like the server does", () => {
    // Dosyanın satırları.
    const lines = paths.trim().split("\n");
    // 485 yol olmalı.
    expect(lines.length).toBe(485);
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
    // Başlangıç.
    expect(dayProgress("08:00")).toBe(0);
    // Öğlen sonrası: sekiz saat geçti.
    expect(dayProgress("16:00")).toBe(0.5);
  });
});
