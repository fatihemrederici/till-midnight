// React araçları.
import { useEffect, useState } from "react";
// Sesler.
import { setDrone, setHeartbeat } from "../audio/ambience";
import { vibrate } from "../audio/haptics";
import { sfx } from "../audio/sfx";
// Tipler.
import type { Language, Story } from "../game/types";
// Dil yardımcısı.
import { say } from "../i18n/languages";
// Yazılar.
import { UI } from "../i18n/ui";

// Bir kehanet cümlesinin ekranda kalma süresi. Uzun cümle okunabilsin diye daha uzun kalır.
// Satırın sönmesi, süresinin sonunda bu kadar sürer.
const FADE_MS = 260;

function lineDuration(line: string): number {
  // En az 1,5 saniye, her harf için biraz daha.
  return Math.max(1500, 900 + line.length * 45);
}

// Kehanet: Felaket kısa cümlelerle ekranda patlar.
export function VisionScreen({
  story,
  language,
  onDone,
}: {
  story: Story;
  language: Language;
  onDone: () => void;
}) {
  // Hangi cümle gösteriliyor. Sonuncudan sonra uyanış.
  const [step, setStep] = useState(0);
  // Son adım uyanış metni.
  const awake = step >= story.vision.length;
  // Şu anki cümle ve süresi.
  const line = awake ? "" : say(story.vision[step], language);
  const duration = lineDuration(line);

  // Ekran açılınca: Güçlü uğultu ve hızlı kalp.
  useEffect(() => {
    setDrone(0.28);
    setHeartbeat(620);
    return () => setHeartbeat(0);
  }, []);

  // Her adımda ses ve zamanlama.
  useEffect(() => {
    // Uyanışta bekleme yok, oyuncu dokunarak devam eder.
    if (awake) {
      // Felaket bitti: Sessizlik.
      setDrone(0.05);
      setHeartbeat(0);
      return;
    }
    // Son cümle felaket sesiyle, diğerleri darbeyle.
    if (step === story.vision.length - 1) {
      sfx.crash();
      vibrate([60, 40, 200]);
    } else {
      sfx.flash();
      vibrate(40);
    }
    // Sıradaki cümle.
    const timer = window.setTimeout(() => setStep((current) => current + 1), duration);
    return () => window.clearTimeout(timer);
  }, [step, awake, story.vision.length, duration]);

  return (
    <div className="vision" data-awake={awake}>
      {awake ? (
        <div className="screen">
          <p className="lede wake">{say(story.wake, language)}</p>
          <button className="go" onClick={onDone}>
            {UI.continue[language]}
          </button>
        </div>
      ) : (
        <>
          {/* Işık patlaması: Sesle aynı anda, her satırda bir kez. key değişince baştan oynar. */}
          <div
            className="vision-flash"
            key={`flash-${step}`}
            data-last={step === story.vision.length - 1}
          />
          {/* Yazı: Hemen çarpar, satırın süresi bitmeden söner. */}
          <p
            className="vision-line"
            key={step}
            data-last={step === story.vision.length - 1}
            style={{ animationDelay: `0ms, ${duration - FADE_MS}ms` }}
          >
            {line}
          </p>
          {/* Kehaneti atlama. */}
          <button className="skip" onClick={() => setStep(story.vision.length)}>
            {UI.skip[language]}
          </button>
        </>
      )}
    </div>
  );
}
