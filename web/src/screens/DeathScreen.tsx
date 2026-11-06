// React araçları.
import { useEffect, useState } from "react";
// Sesler.
import { setDrone, setHeartbeat } from "../audio/ambience";
import { vibrate } from "../audio/haptics";
import { sfx } from "../audio/sfx";
// Dil ve ses düğmeleri.
import { Controls } from "../components/Controls";
// Motor.
import { deathChain } from "../game/engine";
// Tipler.
import type { Ending, GameState, RunStats } from "../game/types";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";
// Ortak özellikler.
import type { ScreenProps } from "./types";

// Zincirin ilk adımından önce beklenen süre ve adımlar arası süre.
const CHAIN_DELAY_MS = 900;
const CHAIN_STEP_MS = 520;
// Veri azken yüzde yanıltıcı olur, bu kadar oyundan sonra gösteriyoruz.
const MIN_RUNS_FOR_SHARE = 20;

// Ölüm ekranı: Zinciri adım adım gösterir.
export function DeathScreen({
  story,
  language,
  onLanguage,
  ending,
  state,
  stats,
  found,
  tries,
  onRewind,
  onDeaths,
  onChapters,
}: ScreenProps & {
  ending: Ending;
  state: GameState;
  stats: RunStats | null;
  found: number;
  // Kaçıncı deneme: Tutanağın numarası.
  tries: number;
  onRewind: () => void;
  onDeaths: () => void;
  onChapters: () => void;
}) {
  // Bu koşuda geçerli zincir adımları.
  const steps = deathChain(story, state.choices);
  // Kaç adım göründü.
  const [shown, setShown] = useState(0);

  // Ekran açılınca: Ölüm sesi ve zincir.
  useEffect(() => {
    // Darbe.
    sfx.death();
    vibrate([120, 60, 260]);
    // Uğultu ve kalp durur: Ölüm.
    setHeartbeat(0);
    setDrone(0.02);
    // Adımları tek tek göster.
    const timers = steps.map((_, i) =>
      window.setTimeout(
        () => {
          setShown(i + 1);
          sfx.chain();
        },
        CHAIN_DELAY_MS + i * CHAIN_STEP_MS,
      ),
    );
    return () => timers.forEach((timer) => window.clearTimeout(timer));
    // Sadece ekran açılınca çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Toplam ölüm sayısı.
  const total = story.deaths.length;
  // Aynı şekilde biten oyunların yüzdesi.
  const share =
    stats && stats.runs >= MIN_RUNS_FOR_SHARE
      ? Math.round((100 * stats.sameEnding) / stats.runs)
      : null;

  // Ölüm saati: Zincirin son adımı, yoksa öldüğün kart.
  const timeOfDeath = steps.at(-1)?.time ?? story.cards[state.index]?.time ?? "";

  return (
    <div className="app dead-screen">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="report-wrap">
        {/* Olay tutanağı: Ne olduğu, ne zaman ve hangi seçimlerin buraya getirdiği. */}
        <article className="report">
          <header className="report-head">
            <span>{UI.report[language]}</span>
            <span>
              {UI.attempt[language]} {tries}
            </span>
          </header>
          <h1 className="report-title strike">{say(ending.title, language)}</h1>
          <p className="report-time">
            {UI.timeOfDeath[language]}: <b>{timeOfDeath}</b>
          </p>
          <p className="report-text">{say(ending.text, language)}</p>
          <p className="chain-label">{UI.chain[language]}</p>
          <ol className="chain">
            {steps.map((step, i) => (
              <li key={i} data-on={i < shown}>
                <span className="chain-time">{step.time}</span> {say(step.text, language)}
              </li>
            ))}
          </ol>
          {/* İpucu en sonda, kenara el yazısıyla düşülmüş bir not gibi gelir. */}
          <p className="could" data-on={shown >= steps.length}>
            {say(ending.hint, language)}
          </p>
          <span className="report-stamp" aria-hidden="true">
            {UI.stamp[language]}
          </span>
        </article>
        <div className="report-actions">
          {share !== null && (
            <p className="small">
              {UI.shareOfPlayers[language].replace("{percent}", String(share))}
            </p>
          )}
          <button className="go" onClick={onRewind}>
            {UI.rewind[language]}
          </button>
          <button className="link" onClick={onDeaths}>
            {UI.deaths[language]} {found}/{total}
          </button>
          <button className="link" onClick={onChapters}>
            {UI.chapters[language]}
          </button>
        </div>
      </div>
    </div>
  );
}
