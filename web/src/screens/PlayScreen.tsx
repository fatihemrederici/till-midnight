// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Göstergeler.
import { Meters } from "../components/Meters";
// Seçimin bedeli ya da kilidi.
import { ChoiceCost } from "../components/ChoiceCost";
// Kaydırılan kart.
import { SwipeCard } from "../components/SwipeCard";
// Motor.
import { canAfford, currentCard } from "../game/engine";
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
  // Şu anki kart: Kriz varsa kriz kartı.
  const card = currentCard(story, state);
  // Saat kriz sırasında son oynanan kartta kalır.
  const time = story.cards[state.lastIndex].time;
  // Bu taraf kilitli mi?
  const isLocked = (side: Side) => !canAfford(card[side], state.meters);
  // Parmak, düğme ve klavye.
  const swipe = useSwipe(onChoose, isLocked);
  // Kalp, uğultu ve darbe.
  const { danger, hit } = useGameFeedback(story, state, card.id);
  // Kaydırılan seçimin etkileyeceği göstergeler.
  const affected = swipe.side ? Object.keys(card[swipe.side].effects ?? {}) : [];

  return (
    <div className="app" data-danger={state.crisis ? "high" : danger}>
      {/* Sarsıntı katmanı: key değişince animasyon baştan başlar. */}
      <div className="play" key={hit.count} data-shake={hit.count > 0}>
        <div className="top">
          {/* key saat değişince animasyonu yeniden başlatır. */}
          <div className="time" key={time}>
            {time}
          </div>
          <Controls language={language} onLanguage={onLanguage} />
        </div>

        {/* Günün ilerlemesi: Hedef gece yarısı. */}
        <div className="day" aria-label={UI.goal[language]}>
          <div
            className="day-fill"
            style={{ width: `${dayProgress(time, story.cards[0].time) * 100}%` }}
          />
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

        <SwipeCard
          card={card}
          crisis={state.crisis !== null}
          flags={state.flags}
          language={language}
          swipe={swipe}
        />

        {/* Düğmeler: Kaydırmak istemeyenler için. */}
        <div className="choices">
          <button
            className="pick l"
            aria-disabled={isLocked("left")}
            onClick={() => swipe.fling("left")}
          >
            ← {say(card.left.label, language)}
            <ChoiceCost story={story} choice={card.left} locked={isLocked("left")} />
          </button>
          <button
            className="pick r"
            aria-disabled={isLocked("right")}
            onClick={() => swipe.fling("right")}
          >
            {say(card.right.label, language)} →
            <ChoiceCost story={story} choice={card.right} locked={isLocked("right")} />
          </button>
        </div>
      </div>
    </div>
  );
}
