// React araçları.
import { useEffect, useRef, useState } from "react";
// Ses etkileri.
import { type Level, setBreathing, setRinging, setWhispering, stopMoods } from "../audio/moods";
// Tipler.
import type { GameState, Meter, Story } from "../game/types";

// Etkilerin başladığı gösterge değeri.
const EFFECT_LIMIT = 2;
// Halüsinasyonun ne zaman başlayıp ne kadar süreceği.
const HALLUCINATION_DELAY_MS = 900;
const HALLUCINATION_MS = 1150;
// Fısıltı, sonuç cümlesi okunduktan sonra gelir.
const WHISPER_DELAY_MS = 1900;
const WHISPER_MS = 3000;

// Bir etkiye bağlı gösterge.
function meterOf(story: Story, effect: Meter["effect"]): Meter | undefined {
  // Bölüm bu etkiyi kullanmıyorsa boş döner.
  return story.meters.find((item) => item.effect === effect);
}

// Sürekli etkinin gücü: Gösterge 1'e düşünce hafif, krizdeyken güçlü.
function levelOf(state: GameState, meter?: Meter): Level {
  // Bölüm bu etkiyi kullanmıyor.
  if (!meter) return 0;
  // Bu göstergenin krizi açık.
  if (state.crisis === meter.id) return 2;
  // Sonuna gelindi.
  return state.meters[meter.id] <= 1 ? 1 : 0;
}

// Rastgele bir sayı: Dil değişse de aynı cümle kalsın diye sıra numarası tutuyoruz.
const randomLine = () => Math.floor(Math.random() * 1000);

// Biten göstergelerin ekrandaki ve sesteki etkisi.
export function useMeterEffects(story: Story, state: GameState) {
  // Göstergeler.
  const mindMeter = meterOf(story, "mind");
  const bodyMeter = meterOf(story, "body");
  const trustMeter = meterOf(story, "trust");
  // Sürekli etkilerin gücü.
  const mind = levelOf(state, mindMeter);
  const body = levelOf(state, bodyMeter);
  const trust = levelOf(state, trustMeter);

  // Tek seferlik göz kırpma: Sayaç değişince yeniden oynar.
  const [blink, setBlink] = useState({ count: 0, deep: false });
  // Tek seferlik renk solması.
  const [fade, setFade] = useState(0);
  // Görünen halüsinasyon ve fısıltının sıra numarası.
  const [hallucination, setHallucination] = useState<number | null>(null);
  const [whisper, setWhisper] = useState<number | null>(null);
  // Bir önceki göstergeler: Neyin düştüğünü anlamak için.
  const previous = useRef(state.meters);

  // Sesleri güce göre ayarla.
  useEffect(() => setRinging(mind), [mind]);
  useEffect(() => setBreathing(body), [body]);
  useEffect(() => setWhispering(trust), [trust]);
  // Oyun ekranından çıkınca hepsini sustur.
  useEffect(() => stopMoods, []);

  // Göstergeler değişince düşüş etkilerini başlat.
  useEffect(() => {
    // Gösterge az önce düştü mü? Etkiler sadece gösterge 2 ve altına inince çalışır.
    const dropped = (meter?: Meter) =>
      meter !== undefined &&
      state.meters[meter.id] < previous.current[meter.id] &&
      state.meters[meter.id] <= EFFECT_LIMIT;
    // Bekleyen zamanlayıcılar.
    const timers: number[] = [];
    // Sağlık: Göz bir kez kapanıp açılır. Sona yaklaştıkça daha derin.
    if (dropped(bodyMeter)) {
      setBlink((current) => ({
        count: current.count + 1,
        deep: state.meters[bodyMeter!.id] <= 1,
      }));
    }
    // Akıl: Kart metni bir anlığına başka bir cümleye dönüşür.
    if (dropped(mindMeter)) {
      timers.push(
        window.setTimeout(() => setHallucination(randomLine()), HALLUCINATION_DELAY_MS),
        window.setTimeout(() => setHallucination(null), HALLUCINATION_DELAY_MS + HALLUCINATION_MS),
      );
    }
    // Güven: Renk solar, sonuç satırında bir fısıltı belirir.
    if (dropped(trustMeter)) {
      setFade((current) => current + 1);
      timers.push(
        window.setTimeout(() => setWhisper(randomLine()), WHISPER_DELAY_MS),
        window.setTimeout(() => setWhisper(null), WHISPER_DELAY_MS + WHISPER_MS),
      );
    }
    previous.current = state.meters;
    // Yeni kart gelirse eski bekleyenler iptal olur.
    return () => {
      timers.forEach((timer) => window.clearTimeout(timer));
      setHallucination(null);
      setWhisper(null);
    };
    // Sadece göstergeler değişince çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.meters]);

  // Ekranın ihtiyacı olanları döndür.
  return { mind, body, trust, blink, fade, hallucination, whisper };
}
