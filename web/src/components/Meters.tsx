// Dil yardımcısı.
import { say } from "../i18n/languages";
// Tipler.
import type { GameState, Language, Story } from "../game/types";

// Üç gösterge: Akıl, sağlık, güven.
export function Meters({
  story,
  state,
  language,
  affected,
  hit,
}: {
  story: Story;
  state: GameState;
  language: Language;
  // Kaydırılan seçimin etkileyeceği göstergeler.
  affected: string[];
  // Az önce düşen göstergeler.
  hit: string[];
}) {
  return (
    <div className="meters">
      {story.meters.map((meter) => {
        // Göstergenin değeri.
        const value = state.meters[meter.id];
        return (
          <div
            className="meter"
            key={meter.id}
            data-hint={affected.includes(meter.id)}
            data-low={value <= 1}
            data-hit={hit.includes(meter.id)}
          >
            <span className="meter-name">
              {meter.emoji} {say(meter.label, language)}
            </span>
            <span className="pips" role="img" aria-label={`${value} / ${story.maxMeter}`}>
              {Array.from({ length: story.maxMeter }, (_, i) => (
                <i className="pip" key={i} data-on={i < value} />
              ))}
            </span>
          </div>
        );
      })}
    </div>
  );
}
