// Test araçları.
import { describe, expect, it } from "vitest";
// Hikâye dosyası.
import storyFile from "../../stories/first-day.json";
// Beklenen yollar: Java testinin kullandığı dosyanın aynısı.
import pathsFile from "../../stories/first-day.paths.json";
// Test edilen motor.
import { cardText, choose, dayProgress, holds, newGame } from "./engine";
// Tipler.
import type { Side, Story } from "./types";

// JSON'u hikâye tipi olarak kullan.
const story = storyFile as unknown as Story;

// Motorun testleri.
describe("engine", () => {
  // Java motoruyla birebir aynı sonuçlar çıkmalı.
  it("ends every known path like the server does", () => {
    // 485 yol olmalı.
    expect(pathsFile.paths.length).toBe(485);
    // Her yolu dene.
    for (const path of pathsFile.paths) {
      // Oyunu başlat.
      let state = newGame(story);
      // Her seçimi uygula.
      for (const letter of path.choices) {
        const side: Side = letter === "L" ? "left" : "right";
        state = choose(story, state, side);
      }
      // Beklenen son.
      expect(state.endingId).toBe(path.ending);
      // Seçimler kaydedilmiş olmalı.
      expect(state.choices.length).toBe(path.choices.length);
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
    // Elif'in haber kartı.
    const card = story.cards.find((item) => item.id === "elif-news")!;
    // Kehaneti biliyorsa farklı cümle.
    expect(cardText(card, { elifKnows: true }).en).toContain("Are we next?");
    // Bilmiyorsa normal cümle.
    expect(cardText(card, {}).en).toContain("coincidence");
  });

  // Saat, gün çubuğuna doğru çevrilmeli.
  it("converts the clock to day progress", () => {
    // Başlangıç.
    expect(dayProgress("08:00")).toBe(0);
    // Öğlen sonrası: sekiz saat geçti.
    expect(dayProgress("16:00")).toBe(0.5);
  });
});
