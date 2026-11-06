// React araçları.
import { useEffect, useRef, useState } from "react";
// Sesler.
import { vibrate } from "../audio/haptics";
import { sfx } from "../audio/sfx";
import { unlockAudio } from "../audio/synth";
// Tip.
import type { Side } from "../game/types";

// Karar eşiği: Kart bu kadar piksel kayarsa seçim yapılmış sayılır.
const THRESHOLD = 80;
// Kartın uçuş süresi. Animasyonla aynı olmalı.
const FLING_MS = 200;

// Hareket azaltma tercihi açık mı?
const prefersCalm = () => window.matchMedia("(prefers-reduced-motion: reduce)").matches;

// Kart kaydırma davranışı: Parmak, düğme ve ok tuşları hepsi buradan geçer.
export function useSwipe(onChoose: (side: Side) => void, isLocked: (side: Side) => boolean) {
  // Kartın konumu: Parmak kadar kayar.
  const [dx, setDx] = useState(0);
  // Parmak kartta mı?
  const [moving, setMoving] = useState(false);
  // Kart uçuyor mu? Uçarken yeni dokunuş alınmaz.
  const [leaving, setLeaving] = useState<Side | null>(null);
  // Parmağın başladığı yer.
  const startX = useRef(0);
  // Karar eşiğini geçti mi? Tık sesi bir kez çalsın diye.
  const past = useRef(false);

  // Kartı uçurur, sonra seçimi uygular.
  const fling = (side: Side) => {
    // Uçarken tekrar seçilemez.
    if (leaving) return;
    // Kilitli seçenek kabul edilmez: Kart yerine döner, oyuncu darbeyi hisseder.
    if (isLocked(side)) {
      sfx.thud();
      vibrate(40);
      setDx(0);
      return;
    }
    // Ses motorunu garantiye al.
    unlockAudio();
    // Hışırtı ve küçük titreşim.
    sfx.swipe();
    vibrate(15);
    setLeaving(side);
    // Animasyon bitince seçimi uygula.
    window.setTimeout(
      () => {
        setLeaving(null);
        setDx(0);
        past.current = false;
        onChoose(side);
      },
      prefersCalm() ? 0 : FLING_MS,
    );
  };

  // Klavyedeki ok tuşları.
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "ArrowLeft") fling("left");
      if (event.key === "ArrowRight") fling("right");
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  // Kartın üzerine bağlanacak parmak olayları.
  const handlers = {
    onPointerDown: (event: React.PointerEvent<HTMLDivElement>) => {
      // Uçarken dokunma yok.
      if (leaving) return;
      setMoving(true);
      startX.current = event.clientX;
      unlockAudio();
      event.currentTarget.setPointerCapture?.(event.pointerId);
    },
    onPointerMove: (event: React.PointerEvent<HTMLDivElement>) => {
      // Parmak kartta değilse geç.
      if (!moving) return;
      const moved = event.clientX - startX.current;
      setDx(moved);
      // Eşik geçilince tık.
      const over = Math.abs(moved) > THRESHOLD;
      if (over && !past.current) {
        sfx.snap();
        vibrate(8);
      }
      past.current = over;
    },
    onPointerUp: () => {
      // Parmak kalktı.
      if (!moving) return;
      setMoving(false);
      if (Math.abs(dx) > THRESHOLD) fling(dx > 0 ? "right" : "left");
      else setDx(0);
    },
    onPointerCancel: () => {
      setMoving(false);
      setDx(0);
    },
  };

  // Kartın anlık dönüşümü.
  const transform = leaving
    ? `translateX(${(leaving === "right" ? 1 : -1) * (window.innerWidth + 150)}px) rotate(${leaving === "right" ? 22 : -22}deg)`
    : `translateX(${dx}px) rotate(${dx / 18}deg)`;

  // Sürüklenen yön. Küçük titremeler sayılmaz.
  const side: Side | null = Math.abs(dx) > 14 ? (dx > 0 ? "right" : "left") : null;

  // Ekranın ihtiyacı olanları döndür.
  return { dx, side, moving, transform, handlers, fling };
}
