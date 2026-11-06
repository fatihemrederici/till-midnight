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
// Biten göstergelerin ekran etkileri.
import { Blink, Eyelids, HeartPulse } from "../components/MeterEffects";
// Saat hesapları.
import { dayProgress, toMinutes } from "../game/clock";
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
// Biten göstergelerin cümleleri.
import { WHISPERS } from "../i18n/whispers";
// Ölüm cümleleri.
import { omenText } from "../i18n/omens";
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
  // Şu anki kart: Kriz varsa kriz kartı.
  const card = currentCard(story, state);
  // Saat kriz sırasında son oynanan kartta kalır.
  const time = story.cards[state.lastIndex].time;
  // Bu taraf kilitli mi?
  const isLocked = (side: Side) => !canAfford(card[side], state.meters);
  // Parmak, düğme ve klavye.
  const swipe = useSwipe(onChoose, isLocked);
  // Son saat: 23:00'ten sonra.
  const late = toMinutes(time) >= LAST_HOUR;
  // Kalp, uğultu ve darbe.
  const { danger, hit } = useGameFeedback(story, state, card.id, late);
  // Biten göstergenin etkisi.
  const effects = useMeterEffects(story, state);
  // Görünen halüsinasyon ve fısıltı cümlesi.
  const hallucination = effects.omen === null ? null : omenText(story.id, effects.omen, language);
  const whisper =
    effects.whisper === null
      ? null
      : `“${WHISPERS[language][effects.whisper % WHISPERS[language].length]}”`;
  // Güven bitince kimse yol göstermez: Seçimin etkisi önceden görünmez.
  const blind = effects.trust > 0;
  // Kaydırılan seçimin etkileyeceği göstergeler.
  const affected = swipe.side && !blind ? Object.keys(card[swipe.side].effects ?? {}) : [];

  return (
    <div
      className="app"
      data-danger={state.crisis ? "high" : danger}
      data-mind={effects.mind}
      data-body={effects.body}
      data-trust={effects.trust}
      data-late={late}
      key={`fade-${effects.fade}`}
      data-fade={effects.fade > 0}
    >
      <Eyelids level={effects.body} />
      <Blink count={effects.blink.count} deep={effects.blink.deep} />
      <HeartPulse active={effects.body > 0} />
      {/* Sarsıntı katmanı: key değişince animasyon baştan başlar. */}
      <div className="play" key={hit.count} data-shake={hit.count > 0}>
        <div className="top">
          {/* key saat değişince animasyonu yeniden başlatır. */}
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
        {/* Güven düşünce sonuç cümlesinden sonra aynı yerde bir fısıltı belirir. */}
        <p
          className="after"
          key={`after-${card.id}-${whisper ? "whisper" : "text"}`}
          data-whisper={whisper !== null}
          aria-live="polite"
        >
          {whisper ?? (state.after ? say(state.after, language) : "")}
        </p>

        <SwipeCard
          card={card}
          crisis={state.crisis !== null}
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
            aria-disabled={isLocked("left")}
            onClick={() => swipe.fling("left")}
            onMouseEnter={() => swipe.setHover("left")}
            onMouseLeave={() => swipe.setHover(null)}
          >
            ← {say(card.left.label, language)}
            <ChoiceCost story={story} choice={card.left} locked={isLocked("left")} hidden={blind} />
          </button>
          <button
            className="pick r"
            aria-disabled={isLocked("right")}
            onClick={() => swipe.fling("right")}
            onMouseEnter={() => swipe.setHover("right")}
            onMouseLeave={() => swipe.setHover(null)}
          >
            {say(card.right.label, language)} →
            <ChoiceCost
              story={story}
              choice={card.right}
              locked={isLocked("right")}
              hidden={blind}
            />
          </button>
        </div>
      </div>
    </div>
  );
}
