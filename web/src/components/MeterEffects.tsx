// React araçları.
import { useEffect, useRef, useState } from "react";
// Tip.
import type { Language } from "../game/types";
// Fısıltılar.
import { WHISPERS } from "../i18n/whispers";

// Ekranda beliren tek bir fısıltı.
type Whisper = { id: number; text: string; top: number; left: number };

// Sağlık bitince göz kapakları ağırlaşır ve kapanıp açılır.
export function Eyelids({ level }: { level: number }) {
  // Etki yoksa çizme.
  if (level === 0) return null;
  // Üst ve alt kapak.
  return (
    <div className="eyelids" data-level={level} aria-hidden="true">
      <i className="lid top" />
      <i className="lid bottom" />
    </div>
  );
}

// Güven bitince ekranın kenarlarında fısıltılar belirir.
export function Whispers({ level, language }: { level: number; language: Language }) {
  // Görünen fısıltılar.
  const [items, setItems] = useState<Whisper[]>([]);
  // Her fısıltıya farklı anahtar. Dil değişse de sayaç sıfırlanmaz.
  const nextId = useRef(0);

  useEffect(() => {
    // Etki yoksa temizle.
    if (level === 0) {
      setItems([]);
      return;
    }
    // Yeni bir fısıltı ekler.
    const add = () => {
      nextId.current += 1;
      // Kartı kapatmasın diye üstte ya da altta çıkar.
      const nearTop = Math.random() < 0.5;
      const texts = WHISPERS[language];
      setItems((current) => [
        ...current.slice(-2),
        {
          id: nextId.current,
          text: texts[Math.floor(Math.random() * texts.length)],
          top: nearTop ? 12 + Math.random() * 8 : 86 + Math.random() * 4,
          left: 6 + Math.random() * 45,
        },
      ]);
    };
    // Hemen başla, sonra tekrarla.
    add();
    const timer = window.setInterval(add, level === 2 ? 1400 : 2600);
    return () => window.clearInterval(timer);
  }, [level, language]);

  // Fısıltıları çiz.
  return (
    <div className="whispers" aria-hidden="true">
      {items.map((item) => (
        <span
          className="whisper"
          key={item.id}
          style={{ top: `${item.top}%`, left: `${item.left}%` }}
        >
          {item.text}
        </span>
      ))}
    </div>
  );
}
