// Test araçları.
import { describe, expect, it } from "vitest";
// Test edilen seçici.
import { armedEndings, omenText, pickOmen } from "./omens";
// Motor.
import { newGame } from "./engine";
// Tipler.
import type { Story } from "./types";

// Tüm bölümlerin kuralları.
const stories = import.meta.glob("../../../stories/*/story.json", { eager: true });
const chapters = Object.values(stories).map((module) => module as unknown as Story);
// İlk Gün.
const day = chapters.find((story) => story.id === "first-day")!;
// Cümlesi olan ölümler: Kurallar dosyasında metin yok, bu yüzden ölümleri doğrudan veriyoruz.
const known = day.endings.filter((ending) => !ending.won).map((ending) => ending.id);

describe("omens", () => {
  // Akıl 1'e inince akıl ölümü yaklaşmış sayılır; ölüm kartın yerine göre.
  it("knows a meter at 1 is close to its death", () => {
    const state = newGame(day);
    const mind = day.meters.find((meter) => meter.id === "mind")!;
    const place = day.cards[state.index].place;
    expect(armedEndings(day, state)).not.toContain(mind.endings[place]);
    const low = { ...state, meters: { ...state.meters, mind: 1 } };
    expect(armedEndings(day, low)).toContain(mind.endings[place]);
  });

  // Gerçeği söyleme anında yaklaşan ölümü seçer, aynı cümleyi tekrar etmez.
  it("sometimes tells the truth and never repeats", () => {
    const state = { ...newGame(day), meters: { mind: 1, body: 6 } };
    const armed = armedEndings(day, state);
    // random 0: Gerçek yolu seçilir.
    const truth = pickOmen(day, state, known, null, () => 0);
    expect("ending" in truth && armed.includes(truth.ending)).toBe(true);
    // Önceki cümle tekrar gelmez.
    for (let i = 0; i < 50; i++) expect(pickOmen(day, state, known, truth)).not.toEqual(truth);
    // Cümlesi olmayan bölümde genel cümle.
    const generic = pickOmen(day, state, [], null, () => 0.5);
    expect(omenText(day, generic, "tr")).not.toBe("");
  });
});
