// React araçları.
import { useEffect } from "react";
// Sesler.
import { setDrone, setHeartbeat } from "../audio/ambience";
import { vibrate } from "../audio/haptics";
import { sfx } from "../audio/sfx";
// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Tipler.
import type { Ending } from "../game/types";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";
// Ortak özellikler.
import type { ScreenProps } from "./types";

// Kurtuluş ekranı.
export function WinScreen({
  language,
  onLanguage,
  ending,
  tries,
  onAgain,
}: Omit<ScreenProps, "story"> & { ending: Ending; tries: number; onAgain: () => void }) {
  // Ekran açılınca: Akor ve sessizlik.
  useEffect(() => {
    sfx.win();
    vibrate(30);
    setHeartbeat(0);
    setDrone(0.03);
  }, []);

  return (
    <div className="app won-screen">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="screen">
        <h1 className="big">{say(ending.title, language)}</h1>
        <p className="lede">{say(ending.text, language)}</p>
        <p className="small">
          {UI.attempt[language]} {tries}. {say(ending.hint, language)}
        </p>
        <button className="go" onClick={onAgain}>
          {UI.again[language]}
        </button>
      </div>
    </div>
  );
}
