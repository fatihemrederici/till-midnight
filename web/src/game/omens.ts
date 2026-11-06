// Akıl düşükken görünen ölüm cümlelerini seçer. Metinlere bakmaz, sadece hangi sonun seçileceğine karar verir.
import { holds } from "./engine";
// Tipler.
import type { GameState, Story } from "./types";

// Bir ölüm cümlesi: Bölümün bir ölümüne ait ya da genel bir cümle.
export type Omen = { ending: string } | { generic: number };

// Yaklaşan ölüm varken gerçeği söyleme ihtimali. Gerisi yanıltır.
const TRUTH = 0.4;

// Şu an gerçekten yaklaşan ölümler.
export function armedEndings(story: Story, state: GameState): string[] {
  // Aynı ölüm iki kez sayılmasın.
  const armed = new Set<string>();
  // Sonuna gelmiş göstergenin ölümü.
  for (const meter of story.meters) {
    if (state.meters[meter.id] <= 1) armed.add(meter.ending);
  }
  // Önümüzdeki kartlardaki ölümcül seçimler.
  story.cards.slice(state.index).forEach((card, offset) => {
    for (const choice of [card.left, card.right]) {
      // Ölümcül değilse geç.
      if (!choice.death) continue;
      // Şartlı ölüm: Önceki seçimlerin kurduğu tuzak.
      const conditional = Object.keys(choice.death.when ?? {}).length > 0;
      // Şartsız ölümü sadece şu anki kartta say, yoksa her şey yakın görünür.
      if ((offset === 0 || conditional) && holds(choice.death.when, state.flags)) {
        armed.add(choice.death.ending);
      }
    }
  });
  return [...armed];
}

// Bir sonraki cümleyi seçer. known: Bölümde cümlesi yazılmış ölümler.
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
