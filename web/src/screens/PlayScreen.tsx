// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Göstergeler.
import { Meters } from "../components/Meters";
// Kaydırılan kart.
import { SwipeCard } from "../components/SwipeCard";
// Saat hesapları.
import { dayProgress } from "../game/clock";
// Tipler.
import type { GameState, Side } from "../game/types";
// Sesli geri bildirim.
import { useGameFeedback } from "../hooks/useGameFeedback";
// Kaydırma davranışı.
import { useSwipe } from "../hooks/useSwipe";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";
// Ortak özellikler.
import type { ScreenProps } from "./types";

// Oyun ekranı: Saat, göstergeler ve kaydırılan kart.
export function PlayScreen({
  story,
  language,
  onLanguage,
  state,
  tries,
  onChoose,
}: ScreenProps & { state: GameState; tries: number; onChoose: (side: Side) => void }) {
  // Şu anki kart.
  const card = story.cards[state.index];
  // Parmak, düğme ve klavye.
  const swipe = useSwipe(onChoose);
  // Kalp, uğultu ve darbe.
  const { danger, hit } = useGameFeedback(story, state, card.id);
  // Kaydırılan seçimin etkileyeceği göstergeler.
  const affected = swipe.side ? Object.keys(card[swipe.side].effects ?? {}) : [];

  return (
    <div className="app" data-danger={danger}>
      {/* Sarsıntı katmanı: key değişince animasyon baştan başlar. */}
      <div className="play" key={hit.count} data-shake={hit.count > 0}>
        <div className="top">
          {/* key saat değişince animasyonu yeniden başlatır. */}
          <div className="time" key={card.time}>
            {card.time}
          </div>
          <Controls language={language} onLanguage={onLanguage} />
        </div>

        {/* Günün ilerlemesi: Hedef gece yarısı. */}
        <div className="day" aria-label={UI.goal[language]}>
          <div className="day-fill" style={{ width: `${dayProgress(card.time) * 100}%` }} />
          <span className="day-label">
            <span>{UI.goal[language]}</span>
            <span>
              {UI.attempt[language]} {tries}
            </span>
          </span>
        </div>

        <Meters story={story} state={state} language={language} affected={affected} hit={hit.ids} />

        {/* Önceki seçimin sonucu. */}
        <p className="after" key={`after-${card.id}`} aria-live="polite">
          {state.after ? say(state.after, language) : ""}
        </p>

        <SwipeCard card={card} flags={state.flags} language={language} swipe={swipe} />

        {/* Düğmeler: Kaydırmak istemeyenler için. */}
        <div className="choices">
          <button className="pick l" onClick={() => swipe.fling("left")}>
            ← {say(card.left.label, language)}
          </button>
          <button className="pick r" onClick={() => swipe.fling("right")}>
            {say(card.right.label, language)} →
          </button>
        </div>
      </div>
    </div>
  );
}
