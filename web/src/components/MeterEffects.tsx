// React araçları.
import { useEffect, useRef } from "react";
// Kalp atışını dinlemek için.
import { onBeat } from "../audio/ambience";
// Göz kapağı sesleri.
import { sfx } from "../audio/sfx";

// Animasyonun süresi ve hangi anında hangi sesin çalacağı. Oranlar effects.css'teki anahtar karelerle aynı.
type Timeline = { ms: number; sounds: [number, () => void][] };
// Uyuklama: 50% ağırlaşmaya başlar, 76% kapanır, 82% irkilerek açılır, 90.5% hızlı kırpar.
const DOZE: Timeline = {
  ms: 6000,
  sounds: [
    [0.5, sfx.exhale],
    [0.76, sfx.lidClose],
    [0.82, sfx.gasp],
    [0.905, sfx.lidClose],
  ],
};
// Tek kırpış: 35% kapanır.
const ONCE: Timeline = { ms: 450, sounds: [[0.33, sfx.lidClose]] };
// Derin kırpış: 25% kapanır, 50% titreyerek açılmaya başlar.
const DEEP: Timeline = {
  ms: 1700,
  sounds: [
    [0.25, sfx.lidClose],
    [0.5, sfx.gasp],
  ],
};

// Sesleri animasyonun kendi saatine bağlar: Her tur başladığında o turun seslerini zamanlar.
// Böylece ses ile göz kapağı hep aynı anda olur; ayrı bir zamanlayıcı gibi kaymaz.
function useLidSounds(timeline: Timeline) {
  // Bekleyen sesler.
  const timers = useRef<number[]>([]);
  // Bileşen kalkınca bekleyenleri iptal et.
  useEffect(() => () => timers.current.forEach((timer) => window.clearTimeout(timer)), []);
  // Bir tur başladı: O turun seslerini zamanla.
  const play = () => {
    timers.current = timeline.sounds.map(([at, sound]) =>
      window.setTimeout(sound, at * timeline.ms),
    );
  };
  // Animasyonun ilk başlangıcı ve her yeni turu.
  return { onAnimationStart: play, onAnimationIteration: play };
}

// Göz kapakları: Üst kapak yolun çoğunu iner, alt kapak az yükselir.
// Kapanınca kapaklardan sızan ışık kırmızı görünür.
function Lids() {
  return (
    <>
      <i className="lid upper" />
      <i className="lid lower" />
      <i className="lid-glow" />
    </>
  );
}

// Sağlık 1'deyken göz ağırlaşır: Yavaşça kapanır, irkilerek açılır, bazen hızlı kırpar.
export function Eyelids({ level }: { level: number }) {
  // Ses, animasyonla aynı anda.
  const sounds = useLidSounds(DOZE);
  // Etki yoksa çizme.
  if (level === 0) return null;
  return (
    <div className="eyelids loop" data-level={level} aria-hidden="true" {...sounds}>
      <Lids />
    </div>
  );
}

// Sağlık düştüğü anda gözler bir kez kırpar. deep: Ağır bir kapanış ve titrek açılış.
export function Blink({ count, deep }: { count: number; deep: boolean }) {
  // Daha düşüş olmadı.
  if (count === 0) return null;
  // key değişince animasyon ve sesler baştan oynar.
  return <OneBlink key={count} deep={deep} />;
}

// Tek bir kırpış ve sesi.
function OneBlink({ deep }: { deep: boolean }) {
  // Ses, animasyonla aynı anda.
  const sounds = useLidSounds(deep ? DEEP : ONCE);
  return (
    <div className="eyelids once" data-deep={deep} aria-hidden="true" {...sounds}>
      <Lids />
    </div>
  );
}

// Sağlık düşükken ekran kenarları kalp sesiyle aynı anda atar: Küt-küt.
export function HeartPulse({ active }: { active: boolean }) {
  // Atan katman.
  const layer = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Etki yoksa dinleme.
    if (!active) return;
    // Hareket istemeyenler için atmaz.
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    // Her atışta iki vuruşluk parlama. İkinci vuruş sesteki ikinci tonla aynı anda.
    return onBeat(() =>
      layer.current?.animate(
        [
          { opacity: 0 },
          { opacity: 0.95, offset: 0.1 },
          { opacity: 0.25, offset: 0.28 },
          { opacity: 0.7, offset: 0.36 },
          { opacity: 0 },
        ],
        { duration: 560, easing: "ease-out" },
      ),
    );
  }, [active]);

  // Etki yoksa çizme.
  if (!active) return null;
  return <div className="heart-pulse" ref={layer} aria-hidden="true" />;
}
