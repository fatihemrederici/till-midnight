// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";
// Ortak özellikler.
import type { ScreenProps } from "./types";

// Bulunan ölümler listesi. Bulunmayanlar gizli kalır.
export function DeathsScreen({
  story,
  language,
  onLanguage,
  found,
  onBack,
}: ScreenProps & { found: string[]; onBack: () => void }) {
  // Sadece ölümler.
  const deaths = story.endings.filter((ending) => !ending.won);
  // Bulunan ölüm sayısı.
  const foundCount = deaths.filter((ending) => found.includes(ending.id)).length;
  return (
    <div className="app">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="screen list-screen">
        <h1 className="big small-big">{UI.deaths[language]}</h1>
        <p className="small">
          {UI.found[language]}: {foundCount}/{deaths.length}
        </p>
        <ul className="death-list">
          {deaths.map((ending) => (
            <li key={ending.id} data-found={found.includes(ending.id)}>
              {found.includes(ending.id) ? say(ending.title, language) : "???"}
            </li>
          ))}
        </ul>
        <button className="go" onClick={onBack}>
          {UI.back[language]}
        </button>
      </div>
    </div>
  );
}
