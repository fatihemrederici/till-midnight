// Ses motoru.
import { unlockAudio } from "../audio/synth";
// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Tipler.
import type { Language, StorySummary } from "../game/types";
// Saklı ilerleme.
import { readFound } from "../hooks/useFoundEndings";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";

// Açılış ekranı: Oyunun adı ve bölümler.
export function TitleScreen({
  chapters,
  language,
  onLanguage,
  onPick,
}: {
  chapters: StorySummary[];
  language: Language;
  onLanguage: (language: Language) => void;
  onPick: (id: string) => void;
}) {
  return (
    <div className="app">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="screen title-screen">
        <h1 className="big">{UI.gameTitle[language]}</h1>
        <p className="lede">{UI.tagline[language]}</p>
        <div className="chapters">
          {chapters.map((chapter) => {
            // Bu bölümde bulunan ölüm sayısı.
            const found = readFound(chapter.id).filter((id) => chapter.deaths.includes(id)).length;
            return (
              <button
                key={chapter.id}
                className="chapter"
                onClick={() => {
                  // Tarayıcı sesi ilk dokunuştan sonra açar.
                  unlockAudio();
                  onPick(chapter.id);
                }}
              >
                <span className="chapter-title">{say(chapter.title, language)}</span>
                <span className="chapter-found">
                  {UI.deaths[language]} {found}/{chapter.deaths.length}
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
