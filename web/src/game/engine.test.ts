// Test araçları.
import { describe, expect, it } from "vitest";
// Saat hesapları.
import { dayProgress } from "./clock";
// Test edilen motor.
import { afterText, cardText, choose, deathChain, holds, newGame } from "./engine";
// Tipler.
import type { Card, Choice, Side, Story, Text } from "./types";

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
    expect(chapters.map((story) => story.id).sort()).toEqual(["first-day"]);
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
  // Küçük bir test bölümü. Metinler sadece zincirde kullanılan "past" alanında var.
  const step = (past: string, extra: object = {}) => ({
    label: text(past),
    past: text(past),
    ...extra,
  });
  const mini = {
    start: { mind: 1, body: 3 },
    maxMeter: 5,
    meters: [
      { id: "mind", endings: { street: "m-street", home: "m-home", work: "m-work" } },
      { id: "body", endings: { street: "b-street", home: "b-home", work: "b-work" } },
    ],
    traps: ["oil"],
    cards: [
      {
        id: "a",
        time: "10:00",
        place: "street",
        left: step("heated oil", { set: { oil: true } }),
        right: step("skipped", { effects: { body: -1 } }),
      },
      {
        id: "b",
        time: "11:00",
        place: "home",
        left: step("answered", { set: { call: true } }),
        right: step("ignored", { effects: { body: -1 } }),
      },
      {
        id: "c",
        time: "12:00",
        place: "home",
        when: { call: true },
        left: step("poured water", { deaths: [{ when: { oil: true }, ending: "fire" }] }),
        right: step("panicked", { effects: { mind: -1 } }),
      },
    ],
    endings: [
      { id: "fire", won: false },
      { id: "m-home", won: false },
      { id: "win", won: true },
    ],
  } as unknown as Story;

  // Gösterge biterse ölüm, o kartın yerine göre seçilir.
  it("ends by place when a meter runs out", () => {
    let state = choose(mini, newGame(mini), "left");
    state = choose(mini, state, "left");
    state = choose(mini, state, "right");
    expect(state.endingId).toBe("m-home");
    expect(story_card(mini, state.index)).toBe("c");
  });

  // Tuzak ölümünün zinciri: Tuzağı kuran, kartı açan ve öldüren seçim, saatleriyle.
  it("explains a trap death with a timed chain", () => {
    const chain = deathChain(mini, ["left", "left", "left"]);
    expect(chain.map((item) => `${item.time} ${item.text.en}`)).toEqual([
      "10:00 heated oil",
      "11:00 answered",
      "12:00 poured water",
    ]);
  });

  // Gösterge ölümünün zinciri: O göstergeyi düşüren seçimler.
  it("explains a meter death with the choices that drained it", () => {
    const chain = deathChain(mini, ["left", "left", "right"]);
    expect(chain.map((item) => item.text.en)).toEqual(["panicked"]);
  });

  // Gerçek bölümde de her ölüm bir zincirle açıklanır.
  it.each(chapters.map((story) => [story.id, story] as const))(
    "explains every death of %s with a chain",
    (id, story) => {
      const path = Object.keys(allPaths).find((key) => key.endsWith(`/${id}.txt`))!;
      for (const line of allPaths[path].trim().split("\n").slice(0, 500)) {
        const [choices, ending] = line.split(" ");
        if (story.endings.find((item) => item.id === ending)?.won) continue;
        const sides = [...choices].map((letter) => (letter === "L" ? "left" : "right") as Side);
        expect(deathChain(story, sides).length, line).toBeGreaterThan(0);
      }
    },
  );

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

  // Seçimin sonucu da önceki seçimlere göre değişebilmeli. Örneğin Emily yanındaysa seni geri çeker.
  it("picks the after text that matches earlier choices", () => {
    const choice = {
      after: text("missed"),
      variants: [{ when: { elif: true }, text: text("pulled back") }],
    } as unknown as Choice;
    expect(afterText(choice, { elif: true })?.en).toBe("pulled back");
    expect(afterText(choice, {})?.en).toBe("missed");
    expect(afterText({} as Choice, {})).toBeNull();
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
