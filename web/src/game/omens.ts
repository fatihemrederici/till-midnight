// Akıl düşükken görünen ölüm cümlelerini seçer. Sadece hangi ölümün seçileceğine karar verir, metni bölüm dosyası verir.
import { holds } from "./engine";
// Tipler.
import type { GameState, Language, Story } from "./types";
// Bölümde cümle yoksa kullanılan genel cümleler.
import { HALLUCINATIONS } from "../i18n/whispers";

// Bir ölüm cümlesi: Bölümün bir ölümüne ait ya da genel bir cümle.
export type Omen = { ending: string } | { generic: number };

// Yaklaşan ölüm varken gerçeği söyleme ihtimali. Gerisi yanıltır.
const TRUTH = 0.4;

// Şu an gerçekten yaklaşan ölümler.
export function armedEndings(story: Story, state: GameState): string[] {
  // Aynı ölüm iki kez sayılmasın.
  const armed = new Set<string>();
  // Kartın yeri: Gösterge ölümü bu yere göre gelir.
  const place = story.cards[state.index]?.place ?? "home";
  // Sonuna gelmiş göstergenin ölümü.
  for (const meter of story.meters) {
    if (state.meters[meter.id] <= 1) armed.add(meter.endings[place]);
  }
  // Önümüzdeki kartlardaki ölümcül seçimler.
  story.cards.slice(state.index).forEach((card, offset) => {
    for (const choice of [card.left, card.right]) {
      for (const death of choice.deaths ?? []) {
        // Şartlı ölüm: Önceki seçimlerin kurduğu tuzak.
        const conditional = Object.keys(death.when ?? {}).length > 0;
        // Şartsız ölümü sadece şu anki kartta say, yoksa her şey yakın görünür.
        if ((offset === 0 || conditional) && holds(death.when, state.flags))
          armed.add(death.ending);
      }
    }
  });
  return [...armed];
}

// Bölümde cümlesi olan ölümler.
export function knownOmens(story: Story): string[] {
  return story.endings.filter((ending) => !ending.won && ending.omen).map((ending) => ending.id);
}

// Bir sonraki cümleyi seçer. known: Cümlesi olan ölümler.
export function pickOmen(
  story: Story,
  state: GameState,
  known: string[],
  previous: Omen | null,
  random: () => number = Math.random,
): Omen {
  // Aynı cümle üst üste gelmesin.
  const fresh = (list: string[]) =>
    list.filter((id) => !(previous && "ending" in previous && previous.ending === id));
  // Listeden rastgele biri.
  const any = (list: string[]) => list[Math.floor(random() * list.length)];
  // Bazen gerçek: Yaklaşan ölümlerden biri.
  const armed = fresh(armedEndings(story, state).filter((id) => known.includes(id)));
  if (armed.length > 0 && random() < TRUTH) return { ending: any(armed) };
  // Çoğu zaman bölümün herhangi bir ölümü. Doğru da olabilir, yanlış da.
  const all = fresh(known);
  if (all.length > 0) return { ending: any(all) };
  // Bölümün cümlesi yoksa genel bir cümle.
  return { generic: Math.floor(random() * 1000) };
}

// Cümlenin seçilen dildeki metni.
export function omenText(story: Story, omen: Omen, language: Language): string {
  // Bölümün kendi cümlesi.
  if ("ending" in omen) {
    const line = story.endings.find((ending) => ending.id === omen.ending)?.omen;
    if (line) return line[language];
  }
  // Genel cümle.
  const lines = HALLUCINATIONS[language];
  return lines[("generic" in omen ? omen.generic : 0) % lines.length];
}
