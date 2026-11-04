// React araçları.
import { useEffect, useState } from "react";
// Sesler.
import { setDrone } from "../audio/ambience";
import { sfx } from "../audio/sfx";
// Saat hesapları.
import { formatMinutes, toMinutes } from "../game/clock";

// Geri sarmanın süresi.
const REWIND_MS = 1100;

// Zamanın geri sarılması: Saat hızla geriye akar, sonra oyun başlar.
export function RewindScreen({
  from,
  to,
  onDone,
}: {
  from: string;
  to: string;
  onDone: () => void;
}) {
  // Ekranda görünen dakika.
  const [now, setNow] = useState(toMinutes(from));

  useEffect(() => {
    // Hareket azaltma açıksa hemen geç.
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      onDone();
      return;
    }
    // Geri sarma sesi.
    sfx.rewind();
    setDrone(0.06);
    // Başlangıç anı ve saatler.
    const started = performance.now();
    const start = toMinutes(from);
    const end = toMinutes(to);
    // Her karede saati güncelle.
    let frame = requestAnimationFrame(function update(time) {
      const progress = Math.min(1, (time - started) / REWIND_MS);
      setNow(Math.round(start + (end - start) * progress * progress));
      if (progress < 1) frame = requestAnimationFrame(update);
      else onDone();
    });
    return () => cancelAnimationFrame(frame);
    // Sadece ekran açılınca çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="rewind">
      <div className="rewind-clock">{formatMinutes(now)}</div>
    </div>
  );
}
