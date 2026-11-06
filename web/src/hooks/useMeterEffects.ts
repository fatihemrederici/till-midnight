// React araçları.
import { useEffect } from "react";
// Ses etkileri.
import { type Level, setBreathing, setRinging, setWhispering, stopMoods } from "../audio/moods";
// Tipler.
import type { GameState, Story } from "../game/types";

// Etkinin gücünü bulur: Gösterge 1'e düşünce hafif, krizdeyken güçlü.
function levelOf(story: Story, state: GameState, effect: "mind" | "body" | "trust"): Level {
  // Bu etkiye bağlı gösterge.
  const meter = story.meters.find((item) => item.effect === effect);
  // Bölüm bu etkiyi kullanmıyor.
  if (!meter) return 0;
  // Bu göstergenin krizi açık.
  if (state.crisis === meter.id) return 2;
  // Sonuna gelindi.
  return state.meters[meter.id] <= 1 ? 1 : 0;
}

// Biten göstergenin ekrandaki ve sesteki etkisi.
export function useMeterEffects(story: Story, state: GameState) {
  // Üç etkinin gücü.
  const mind = levelOf(story, state, "mind");
  const body = levelOf(story, state, "body");
  const trust = levelOf(story, state, "trust");

  // Sesleri güce göre ayarla.
  useEffect(() => setRinging(mind), [mind]);
  useEffect(() => setBreathing(body), [body]);
  useEffect(() => setWhispering(trust), [trust]);

  // Oyun ekranından çıkınca hepsini sustur.
  useEffect(() => stopMoods, []);

  // Ekranın ihtiyacı olanları döndür.
  return { mind, body, trust };
}
