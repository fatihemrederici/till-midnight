// React araçları.
import { useEffect, useRef, useState } from "react";
// Ses etkileri.
import { type Level, setBreathing, setRinging, setWhispering, stopMoods } from "../audio/moods";
// Ölüm cümlesi seçici.
import { type Omen, pickOmen } from "../game/omens";
// Bölümün ölüm cümleleri.
import { knownOmens } from "../i18n/omens";
// Parazit süresi.
import { STATIC_MS } from "../components/TvStatic";
// Tipler.
import type { GameState, Meter, Story } from "../game/types";

// Etkilerin başladığı gösterge değeri.
const EFFECT_LIMIT = 2;
// Akıl düştüğü an: Sonuç cümlesi okunduktan sonra bir cümle.
const DROP_DELAY_MS = 900;
const DROP_HOLD_MS = 900;
// Akıl 1'deyken: Her beş saniyede bir kısa cümle. Kart okunabilsin diye kısa kalır.
const OMEN_EVERY_MS = 5000;
const OMEN_HOLD_MS = 800;
// Parazitin ortasında yazı değişir, böylece değişim görünmez.
const SWAP_MS = 110;
// Yalan söyleyen saat: İki cümlenin arasında, bazen.
const LIE_AT_MS = 2500;
const LIE_MS = 450;
const LIE_CHANCE = 0.35;
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

// Görünen parazit: id her seferinde yenidir, sound girişte cızırdar.
export type StaticBurst = { id: number; sound: boolean };

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
  // Görünen ölüm cümlesi, yazının üstündeki parazit ve yalan söyleyen saat.
  const [omen, setOmen] = useState<Omen | null>(null);
  const [burst, setBurst] = useState<StaticBurst | null>(null);
  const [clockLie, setClockLie] = useState(false);
  // Görünen fısıltının sıra numarası.
  const [whisper, setWhisper] = useState<number | null>(null);
  // Bir önceki göstergeler: Neyin düştüğünü anlamak için.
  const previous = useRef(state.meters);
  // Zamanlayıcılar en güncel durumu okusun diye.
  const latest = useRef(state);
  latest.current = state;
  // Son cümle: Aynısı üst üste gelmesin.
  const lastOmen = useRef<Omen | null>(null);
  // Parazitlere sıra numarası.
  const burstId = useRef(0);

  // Yazı paraziti ile kartın bir an başka bir cümleye dönüşmesi.
  const hallucinate = (timers: number[], start: number, hold: number) => {
    // Parazit başlar, yazı parazitin altında değişir, parazit kalkar.
    const glitch = (at: number, sound: boolean, change: () => void) => {
      timers.push(
        window.setTimeout(() => setBurst({ id: ++burstId.current, sound }), at),
        window.setTimeout(change, at + SWAP_MS),
        window.setTimeout(() => setBurst(null), at + STATIC_MS),
      );
    };
    // Giriş: Yeni cümle o an seçilir, böylece en güncel durumu bilir.
    glitch(start, true, () => {
      const next = pickOmen(story, latest.current, knownOmens(story.id), lastOmen.current);
      lastOmen.current = next;
      setOmen(next);
    });
    // Dönüş: Kartın kendi metni.
    glitch(start + hold, false, () => setOmen(null));
  };

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
    // Akıl: Kart metni bir anlığına bir ölüm cümlesine dönüşür.
    if (dropped(mindMeter)) hallucinate(timers, DROP_DELAY_MS, DROP_HOLD_MS);
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
      setOmen(null);
      setBurst(null);
      setWhisper(null);
    };
    // Sadece göstergeler değişince çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.meters]);

  // Akıl 1'deyken: Her beş saniyede bir ölüm cümlesi, arada bazen saat yalan söyler.
  // Yeni kart gelince sayaç baştan başlar: Oyuncu okumaya başlar başlamaz cümle çıkmaz.
  const cardKey = `${state.index}-${state.crisis}`;
  useEffect(() => {
    // Akıl yerindeyse bir şey yok.
    if (mind === 0) return;
    // Bekleyen zamanlayıcılar.
    const timers: number[] = [];
    const cycle = () => {
      hallucinate(timers, 0, OMEN_HOLD_MS);
      if (Math.random() < LIE_CHANCE) {
        timers.push(
          window.setTimeout(() => setClockLie(true), LIE_AT_MS),
          window.setTimeout(() => setClockLie(false), LIE_AT_MS + LIE_MS),
        );
      }
    };
    const interval = window.setInterval(cycle, OMEN_EVERY_MS);
    return () => {
      window.clearInterval(interval);
      timers.forEach((timer) => window.clearTimeout(timer));
      setOmen(null);
      setBurst(null);
      setClockLie(false);
    };
    // Akıl ya da kart değişince yeniden kur.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mind, cardKey]);

  // Ekranın ihtiyacı olanları döndür.
  return { mind, body, trust, blink, fade, omen, burst, clockLie, whisper };
}
