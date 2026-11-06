// React araçları.
import { useEffect, useRef, useState } from "react";
// Ses.
import { sfx } from "../audio/sfx";

// Parazitin süresi.
const STATIC_MS = 320;
// Düşük çözünürlük: Büyütülünce eski televizyon karıncası gibi görünür.
const WIDTH = 96;
const HEIGHT = 60;
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

// Kısa süreli, renkli televizyon paraziti. Altındaki kartı kaplar, sonra kaybolur.
export function TvStatic() {
  // Çizilecek yüzey.
  const canvas = useRef<HTMLCanvasElement>(null);
  // Süre doldu mu?
  const [done, setDone] = useState(false);

  useEffect(() => {
    // Ses: Televizyon cızırtısı.
    sfx.static();
    // Çizim alanı.
    const context = canvas.current?.getContext("2d");
    if (!context) return;
    // Başlangıç anı.
    const started = performance.now();
    // Her karede yeni karınca çiz.
    let frame = requestAnimationFrame(function draw(time) {
      for (let y = 0; y < HEIGHT; y++) {
        // Bazı satırlar sinyal kaymasıyla tek renk çizgi olur.
        const band =
          Math.random() < 0.05 ? COLORS[Math.floor(Math.random() * COLORS.length)] : null;
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
  }, []);

  // Süre dolduysa çizme.
  if (done) return null;
  return (
    <canvas className="tv-static" ref={canvas} width={WIDTH} height={HEIGHT} aria-hidden="true" />
  );
}
