// Tipler.
import type { Card, Condition, Language } from "../game/types";
// Televizyon paraziti.
import { TvStatic } from "./TvStatic";
// Motor.
import { cardText } from "../game/engine";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Kaydırma davranışının tipi.
import type { useSwipe } from "../hooks/useSwipe";

// Kartın ihtiyacı olan kaydırma bilgileri.
type Swipe = Pick<ReturnType<typeof useSwipe>, "dx" | "moving" | "transform" | "handlers">;

// Parmakla kaydırılan kart ve arkasındaki kart.
export function SwipeCard({
  card,
  crisis,
  hallucination,
  flags,
  language,
  swipe,
}: {
  card: Omit<Card, "time">;
  crisis: boolean;
  // Akıl düşünce kart metninin yerine bir anlığına geçen cümle.
  hallucination: string | null;
  flags: Condition;
  language: Language;
  swipe: Swipe;
}) {
  // Kaydırma miktarı.
  const { dx } = swipe;
  return (
    <div className="table">
      <div className="card back" aria-hidden="true" />
      <div
        className="card live"
        key={card.id}
        data-moving={swipe.moving}
        data-crisis={crisis}
        style={{ transform: swipe.transform }}
        {...swipe.handlers}
      >
        {/* Halüsinasyon geldiğinde kart bir an televizyon paraziti olur. */}
        {hallucination !== null && <TvStatic />}
        <div className="who">{say(card.who, language)}</div>
        <div className="face" aria-hidden="true">
          {card.emoji}
        </div>
        <p className="text" data-hallucination={hallucination !== null}>
          {hallucination ?? say(cardText(card, flags), language)}
        </p>
        {/* Sürüklerken seçimin yazısı. Kart sola kayarken sağ kenarı ekranda kalır, bu yüzden yazı ters kenarda. */}
        <div className="tag left" style={{ opacity: dx < 0 ? Math.min(1, -dx / 60) : 0 }}>
          {say(card.left.label, language)}
        </div>
        <div className="tag right" style={{ opacity: dx > 0 ? Math.min(1, dx / 60) : 0 }}>
          {say(card.right.label, language)}
        </div>
      </div>
    </div>
  );
}
