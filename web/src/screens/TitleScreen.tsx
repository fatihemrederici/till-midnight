// Ses motoru.
import { unlockAudio } from "../audio/synth";
// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";
// Ortak özellikler.
import type { ScreenProps } from "./types";

// Açılış ekranı.
export function TitleScreen({
  story,
  language,
  onLanguage,
  found,
  onStart,
  onDeaths,
}: ScreenProps & { found: number; onStart: () => void; onDeaths: () => void }) {
  // Toplam ölüm sayısı.
  const total = story.endings.filter((ending) => !ending.won).length;
  return (
    <div className="app">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="screen title-screen">
        <h1 className="big">{say(story.title, language)}</h1>
        <p className="lede">{UI.tagline[language]}</p>
        <button
          className="go"
          onClick={() => {
            // Tarayıcı sesi ilk dokunuştan sonra açar.
            unlockAudio();
            onStart();
          }}
        >
          {UI.start[language]}
        </button>
        {/* Bulunan ölümler listesi. */}
        <button className="link" onClick={onDeaths}>
          {UI.deaths[language]} {found}/{total}
        </button>
      </div>
    </div>
  );
}
