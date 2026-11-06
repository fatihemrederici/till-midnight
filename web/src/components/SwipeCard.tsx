// Tipler.
import type { Card, Condition, Language } from "../game/types";
// Televizyon paraziti.
import { TvStatic } from "./TvStatic";
// Parazitin tipi.
import type { StaticBurst } from "../hooks/useMeterEffects";
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
  burst,
  flags,
  language,
  swipe,
}: {
  card: Omit<Card, "time">;
  crisis: boolean;
  // Akıl düşünce kart metninin yerine bir anlığına geçen cümle.
  hallucination: string | null;
  // Yazının üstündeki parazit.
  burst: StaticBurst | null;
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
        <div className="who">{say(card.who, language)}</div>
        <div className="face" aria-hidden="true">
          {card.emoji}
        </div>
        {/* Yazı bir anlığına parazitlenir ve başka bir cümleye dönüşür. */}
        <div className="text-box">
          {/* Kartın metni yerini korur, böylece kart boyu değişmez. Cümle onun üstüne yazılır. */}
          <p className="text" data-hidden={hallucination !== null}>
            {say(cardText(card, flags), language)}
          </p>
          {hallucination !== null && <p className="text omen">{hallucination}</p>}
          {burst && <TvStatic key={burst.id} sound={burst.sound} />}
        </div>
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
