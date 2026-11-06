// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Göstergeler.
import { Meters } from "../components/Meters";
// Kaydırılan kart.
import { SwipeCard } from "../components/SwipeCard";
// Motor.
import { currentCard } from "../game/engine";
// Biten göstergelerin ekran etkileri.
import { Blink, Eyelids, HeartPulse } from "../components/MeterEffects";
// Saat hesapları.
import { dayProgress, toMinutes } from "../game/clock";
// Ölüm cümlesinin metni.
import { omenText } from "../game/omens";
// Tipler.
import type { GameState, Side } from "../game/types";
// Sesli geri bildirim.
import { useGameFeedback } from "../hooks/useGameFeedback";
// Biten göstergelerin etkisi.
import { useMeterEffects } from "../hooks/useMeterEffects";
// Kaydırma davranışı.
import { useSwipe } from "../hooks/useSwipe";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";
// Ortak özellikler.
import type { ScreenProps } from "./types";

// Son saatin başladığı dakika: 23:00.
const LAST_HOUR = 23 * 60;

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
  const card = currentCard(story, state);
  // Kartın saati.
  const time = card.time;
  // Parmak, düğme ve klavye.
  const swipe = useSwipe(onChoose);
  // Son saat: 23:00'ten sonra.
  const late = toMinutes(time) >= LAST_HOUR;
  // Kalp, uğultu ve darbe.
  const { danger, hit } = useGameFeedback(story, state, card.id, late);
  // Biten göstergenin etkisi.
  const effects = useMeterEffects(story, state);
  // Görünen ölüm cümlesi.
  const hallucination = effects.omen === null ? null : omenText(story, effects.omen, language);
  // Kaydırılan seçimin etkileyeceği göstergeler.
  const affected = swipe.side ? Object.keys(card[swipe.side].effects ?? {}) : [];

  return (
    <div
      className="app"
      data-danger={danger}
      data-mind={effects.mind}
      data-body={effects.body}
      data-late={late}
    >
      <Eyelids level={effects.body} />
      <Blink count={effects.blink.count} deep={effects.blink.deep} />
      <HeartPulse active={effects.body > 0} />
      {/* Sarsıntı katmanı: key değişince animasyon baştan başlar. */}
      <div className="play" key={hit.count} data-shake={hit.count > 0}>
        <div className="top">
          {/* Akıl bitince saat bir an gece yarısını gösterir. */}
          <div className="time" key={time} data-lie={effects.clockLie}>
            {effects.clockLie ? "00:00" : time}
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
          hallucination={hallucination}
          burst={effects.burst}
          flags={state.flags}
          language={language}
          swipe={swipe}
        />

        {/* Düğmeler: Kaydırmak istemeyenler için. */}
        <div className="choices">
          <button
            className="pick l"
            onClick={() => swipe.fling("left")}
            onMouseEnter={() => swipe.setHover("left")}
            onMouseLeave={() => swipe.setHover(null)}
          >
            ← {say(card.left.label, language)}
          </button>
          <button
            className="pick r"
            onClick={() => swipe.fling("right")}
            onMouseEnter={() => swipe.setHover("right")}
            onMouseLeave={() => swipe.setHover(null)}
          >
            {say(card.right.label, language)} →
          </button>
        </div>
      </div>
    </div>
  );
}
