// React durumu.
import { useState } from "react";
// Ses ayarı.
import { isMuted, setMuted, unlockAudio } from "../audio/synth";
// Efekt ayarı.
import { isLowEffects, setLowEffects } from "../data/effects";
// Tip.
import type { Language } from "../game/types";
// Dil listesi.
import { LANGUAGES } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";

// Dil ve ses düğmeleri. Her ekranda aynı yerde.
export function Controls({
  language,
  onLanguage,
}: {
  language: Language;
  onLanguage: (language: Language) => void;
}) {
  // Düğmenin görünüşü için sessizlik durumu.
  const [muted, setMutedState] = useState(isMuted());
  // Efektler azaltılmış mı?
  const [low, setLow] = useState(isLowEffects());

  // Sesi açıp kapatır.
  const toggle = () => {
    // Ses motorunu başlat.
    unlockAudio();
    // Yeni değer.
    setMuted(!muted);
    setMutedState(!muted);
  };

  // Düğmeleri çiz.
  return (
    <div className="controls">
      {/* Dil seçici: Dokununca tüm yazılar hemen değişir. */}
      <div className="languages" role="group" aria-label={UI.language[language]}>
        {LANGUAGES.map((item) => (
          <button
            key={item.id}
            className="language"
            aria-pressed={item.id === language}
            onClick={() => onLanguage(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>
      {/* Efekt düğmesi: Göz kapakları ve titremeler rahatsız edenler için. */}
      <button
        className="sound"
        onClick={() => {
          setLowEffects(!low);
          setLow(!low);
        }}
        aria-pressed={!low}
        aria-label={low ? UI.effectsLow[language] : UI.effectsFull[language]}
        title={low ? UI.effectsLow[language] : UI.effectsFull[language]}
      >
        {low ? "◌" : "✦"}
      </button>
      {/* Ses düğmesi. */}
      <button
        className="sound"
        onClick={toggle}
        aria-label={muted ? UI.soundOff[language] : UI.soundOn[language]}
      >
        {muted ? "🔇" : "🔊"}
      </button>
    </div>
  );
}
