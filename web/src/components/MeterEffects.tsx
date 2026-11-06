// React araçları.
import { useEffect, useRef } from "react";
// Kalp atışını dinlemek için.
import { onBeat } from "../audio/ambience";

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
  // Etki yoksa çizme.
  if (level === 0) return null;
  return (
    <div className="eyelids loop" data-level={level} aria-hidden="true">
      <Lids />
    </div>
  );
}

// Sağlık düştüğü anda gözler bir kez kırpar. deep: Ağır bir kapanış ve titrek açılış.
export function Blink({ count, deep }: { count: number; deep: boolean }) {
  // Daha düşüş olmadı.
  if (count === 0) return null;
  // key değişince animasyon baştan oynar.
  return (
    <div className="eyelids once" key={count} data-deep={deep} aria-hidden="true">
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
