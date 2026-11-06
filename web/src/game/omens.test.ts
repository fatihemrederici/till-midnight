// Test araçları.
import { describe, expect, it } from "vitest";
// Test edilen seçici.
import { armedEndings, pickOmen } from "./omens";
// Motor.
import { newGame } from "./engine";
// Cümleler.
import { knownOmens, OMENS, omenText } from "../i18n/omens";
// Tipler.
import type { Story } from "./types";

// Tüm bölümlerin kuralları.
const stories = import.meta.glob("../../../stories/*/story.json", { eager: true });
const chapters = Object.values(stories).map((module) => module as unknown as Story);
// Son Dans: Etkileri olan bölüm.
const dance = chapters.find((story) => story.id === "last-dance")!;

describe("omens", () => {
  // Yeni bölüm eklenince cümlesi de yazılmalı.
  it("has a line for every death in every language", () => {
    for (const story of chapters) {
      for (const ending of story.endings.filter((item) => !item.won)) {
        const line = OMENS[story.id]?.[ending.id];
        expect(line, `${story.id}/${ending.id}`).toBeDefined();
        for (const language of ["en", "tr", "pl"] as const) expect(line[language]).not.toBe("");
      }
    }
  });

  // Gösterge 1'e inince kendi ölümü yaklaşmış sayılır.
  it("knows a meter at 1 is close to its death", () => {
    const state = newGame(dance);
    const nerve = dance.meters.find((meter) => meter.effect === "mind")!;
    expect(armedEndings(dance, state)).not.toContain(nerve.ending);
    const low = { ...state, meters: { ...state.meters, [nerve.id]: 1 } };
    expect(armedEndings(dance, low)).toContain(nerve.ending);
  });

  // Gerçeği söyleme anında yaklaşan ölümü seçer, aynı cümleyi tekrar etmez.
  it("sometimes tells the truth and never repeats", () => {
    const state = newGame(dance);
    const nerve = dance.meters.find((meter) => meter.effect === "mind")!;
    const low = { ...state, meters: { ...state.meters, [nerve.id]: 1 } };
    const known = knownOmens(dance.id);
    const armed = armedEndings(dance, low);
    // random 0: Gerçek yolu seçilir.
    const truth = pickOmen(dance, low, known, null, () => 0);
    expect("ending" in truth && armed.includes(truth.ending)).toBe(true);
    // Önceki cümle tekrar gelmez.
    for (let i = 0; i < 50; i++) {
      const next = pickOmen(dance, low, known, truth);
      expect(next).not.toEqual(truth);
    }
    // Cümlesi olmayan bölümde genel cümle.
    const generic = pickOmen(dance, low, [], null, () => 0.5);
    expect(omenText("none", generic, "tr")).not.toBe("");
  });
});
