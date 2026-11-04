// React araçları.
import { useEffect, useRef, useState } from "react";
// Sesler.
import { setDrone, setHeartbeat } from "../audio/ambience";
import { vibrate } from "../audio/haptics";
import { sfx } from "../audio/sfx";
// Tipler.
import type { GameState, Story } from "../game/types";

// Tehlike düzeyi: Bir gösterge ne kadar düşükse o kadar yüksek.
export type Danger = "none" | "low" | "high";

// En düşük göstergeye göre tehlike düzeyi.
function dangerOf(lowest: number): Danger {
  // 1 ve altı yüksek, 2 düşük.
  return lowest <= 1 ? "high" : lowest === 2 ? "low" : "none";
}

// Tehlikeye göre sesler.
const HEARTBEAT_MS = { none: 0, low: 1150, high: 640 };
const DRONE_LEVEL = { none: 0.06, low: 0.1, high: 0.16 };

// Oyuncuya durumu hissettiren sesler: Kalp, uğultu, darbe.
export function useGameFeedback(story: Story, state: GameState, cardId: string) {
  // En düşük gösterge.
  const lowest = Math.min(...story.meters.map((meter) => state.meters[meter.id]));
  // Tehlike düzeyi.
  const danger = dangerOf(lowest);
  // Bir önceki kartın göstergeleri: Neyin değiştiğini anlamak için.
  const previous = useRef(state.meters);
  // Az önce düşen göstergeler. Sayaç, aynı göstergenin art arda düşüşünde animasyonu yeniden başlatır.
  const [hit, setHit] = useState({ ids: [] as string[], count: 0 });

  // Tehlikeye göre kalp ve uğultu.
  useEffect(() => {
    setHeartbeat(HEARTBEAT_MS[danger]);
    setDrone(DRONE_LEVEL[danger]);
    return () => setHeartbeat(0);
  }, [danger]);

  // Her yeni kartta saat sesi ve göstergelerin geri bildirimi.
  useEffect(() => {
    sfx.tick();
    // Düşen ve yükselen göstergeler.
    const dropped = story.meters
      .filter((meter) => state.meters[meter.id] < previous.current[meter.id])
      .map((meter) => meter.id);
    const raised = story.meters.some(
      (meter) => state.meters[meter.id] > previous.current[meter.id],
    );
    previous.current = state.meters;
    // Düşüş hissedilsin: Darbe, titreşim ve ekran sarsıntısı.
    if (dropped.length > 0) {
      sfx.thud();
      vibrate(50);
      setHit((current) => ({ ids: dropped, count: current.count + 1 }));
    } else if (raised) {
      // Yükseliş küçük bir nefes gibi.
      sfx.relief();
    }
    // Sadece kart değişince çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cardId]);

  // Ekranın ihtiyacı olanları döndür.
  return { danger, hit };
}
