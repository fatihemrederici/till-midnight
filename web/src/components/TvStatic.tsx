// React araçları.
import { useEffect, useRef, useState } from "react";
// Ses.
import { sfx } from "../audio/sfx";

// Parazitin süresi.
export const STATIC_MS = 240;
// Düşük çözünürlük: Büyütülünce eski televizyon karıncası gibi görünür.
const WIDTH = 120;
// Renkli parazitin renkleri.
const COLORS = [
  "#ff0040",
  "#00ff66",
  "#2060ff",
  "#00e5ff",
  "#ff00d0",
  "#ffe600",
  "#ffffff",
  "#000000",
];

// Kısa süreli, renkli televizyon paraziti. Sadece altındaki yazıyı kaplar, sonra kaybolur.
export function TvStatic({ sound }: { sound: boolean }) {
  // Çizilecek yüzey.
  const canvas = useRef<HTMLCanvasElement>(null);
  // Süre doldu mu?
  const [done, setDone] = useState(false);

  useEffect(() => {
    // Ses: Televizyon cızırtısı. Dönüşte sessiz, yoksa her beş saniyede iki kez cızırdar.
    if (sound) sfx.static();
    // Yüzey ve çizim alanı.
    const element = canvas.current;
    const context = element?.getContext("2d");
    if (!element || !context) return;
    // Yazının en-boy oranına göre piksel sayısı.
    const height = Math.max(
      8,
      Math.round((WIDTH * element.clientHeight) / Math.max(1, element.clientWidth)),
    );
    element.width = WIDTH;
    element.height = height;
    // Başlangıç anı.
    const started = performance.now();
    // Her karede yeni karınca çiz.
    let frame = requestAnimationFrame(function draw(time) {
      for (let y = 0; y < height; y++) {
        // Bazı satırlar sinyal kaymasıyla tek renk çizgi olur.
        const band =
          Math.random() < 0.06 ? COLORS[Math.floor(Math.random() * COLORS.length)] : null;
        for (let x = 0; x < WIDTH; x++) {
          context.fillStyle = band ?? COLORS[Math.floor(Math.random() * COLORS.length)];
          context.fillRect(x, y, 1, 1);
        }
      }
      // Süre dolunca dur.
      if (time - started < STATIC_MS) frame = requestAnimationFrame(draw);
      else setDone(true);
    });
    return () => cancelAnimationFrame(frame);
    // Sadece ilk açılışta.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Süre dolduysa çizme.
  if (done) return null;
  return <canvas className="tv-static" ref={canvas} aria-hidden="true" />;
}
