// React araçları.
import { useEffect, useRef, useState } from "react";
// Sesler.
import { setDrone, setHeartbeat, sfx, unlockAudio, vibrate } from "./audio";
// Motor.
import { cardText, dayProgress, holds, say } from "./engine";
// Yazılar.
import { UI } from "./i18n";
// Dil ve ses düğmeleri.
import { Controls } from "./Header";
// Tipler.
import type { Ending, GameState, Language, RunStats, Side, Story } from "./types";

// Her ekranın ortak aldığı şeyler.
type Common = { story: Story; language: Language; onLanguage: (language: Language) => void };

// Hareket azaltma tercihi açık mı?
const calm = () => window.matchMedia("(prefers-reduced-motion: reduce)").matches;

// Açılış ekranı.
export function TitleScreen({ story, language, onLanguage, found, onStart, onDeaths }: Common & { found: number; onStart: () => void; onDeaths: () => void }) {
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

// Kehanet: Felaket kısa cümlelerle ekranda patlar.
export function VisionScreen({ story, language, onDone }: { story: Story; language: Language; onDone: () => void }) {
  // Hangi cümle gösteriliyor. Sonuncudan sonra uyanış.
  const [step, setStep] = useState(0);
  // Son adım uyanış metni.
  const awake = step >= story.vision.length;

  // Her adımda ses ve zamanlama.
  useEffect(() => {
    // Uğultu kehanette güçlü olsun.
    setDrone(0.28);
    // Kalp hızlı atsın.
    setHeartbeat(620);
    return () => setHeartbeat(0);
  }, []);

  useEffect(() => {
    // Uyanışta bekleme yok, oyuncu dokunarak devam eder.
    if (awake) {
      // Felaket bitti: sessizlik.
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
    const timer = window.setTimeout(() => setStep((current) => current + 1), 1500);
    return () => window.clearTimeout(timer);
  }, [step, awake, story.vision.length]);

  return (
    <div className="vision" data-awake={awake}>
      {/* key değişince animasyon baştan başlar. */}
      {!awake && (
        <p className="vision-line" key={step} data-last={step === story.vision.length - 1}>
          {say(story.vision[step], language)}
        </p>
      )}
      {awake && (
        <div className="screen">
          <p className="lede wake">{say(story.wake, language)}</p>
          <button className="go" onClick={onDone}>
            {UI.continue[language]}
          </button>
        </div>
      )}
      {/* Kehaneti atlama. */}
      {!awake && (
        <button className="skip" onClick={() => setStep(story.vision.length)}>
          {UI.skip[language]}
        </button>
      )}
    </div>
  );
}

// Oyun ekranı: Saat, göstergeler ve kaydırılan kart.
export function PlayScreen({ story, language, onLanguage, state, tries, onChoose }: Common & { state: GameState; tries: number; onChoose: (side: Side) => void }) {
  // Şu anki kart.
  const card = story.cards[state.index];
  // Kartın konumu: Parmak kadar kayar.
  const [dx, setDx] = useState(0);
  // Parmak kartta mı?
  const dragging = useRef(false);
  // Parmağın başladığı yer.
  const startX = useRef(0);
  // Kart uçuyor mu? Uçarken yeni dokunuş alınmaz.
  const [leaving, setLeaving] = useState<Side | null>(null);
  // Karar eşiğini geçti mi? Tık sesi bir kez çalsın diye.
  const past = useRef(false);

  // Karar eşiği: Kartın dörtte biri.
  const threshold = 80;
  // Sürüklenen yön.
  const side: Side | null = Math.abs(dx) > 14 ? (dx > 0 ? "right" : "left") : null;
  // Bu yönün etkilediği göstergeler.
  const affected = side ? Object.keys(card[side].effects ?? {}) : [];
  // En düşük gösterge.
  const lowest = Math.min(...story.meters.map((meter) => state.meters[meter.id]));

  // Tehlikeye göre kalp ve uğultu.
  useEffect(() => {
    // Bir gösterge bitmek üzereyse hızlı kalp.
    setHeartbeat(lowest <= 1 ? 640 : lowest === 2 ? 1150 : 0);
    // Tehlike arttıkça uğultu güçlenir.
    setDrone(lowest <= 1 ? 0.16 : lowest === 2 ? 0.1 : 0.06);
    return () => setHeartbeat(0);
  }, [lowest]);

  // Bir önceki kartın göstergeleri: Neyin değiştiğini anlamak için.
  const previous = useRef(state.meters);
  // Az önce düşen göstergeler. Sayaç aynı göstergenin art arda düşüşünde animasyonu yeniden başlatır.
  const [hit, setHit] = useState({ ids: [] as string[], count: 0 });

  // Her yeni kartta saat sesi ve göstergelerin geri bildirimi.
  useEffect(() => {
    sfx.tick();
    // Düşen ve yükselen göstergeler.
    const dropped = story.meters.filter((meter) => state.meters[meter.id] < previous.current[meter.id]).map((meter) => meter.id);
    const raised = story.meters.some((meter) => state.meters[meter.id] > previous.current[meter.id]);
    previous.current = state.meters;
    // Düşüş hissedilsin: Darbe, titreşim ve ekran sarsıntısı.
    if (dropped.length > 0) {
      sfx.thud();
      vibrate(50);
      setHit((current) => ({ ids: dropped, count: current.count + 1 }));
    } else if (raised) {
      // Yükseliş küçük bir nefes gibi.
      sfx.relief();
    }
    // Sadece kart değişince çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [card.id]);

  // Kartı uçurur, sonra seçimi uygular.
  const fling = (chosen: Side) => {
    // Uçarken tekrar seçilemez.
    if (leaving) return;
    // Ses motorunu garantiye al.
    unlockAudio();
    // Hışırtı ve küçük titreşim.
    sfx.swipe();
    vibrate(15);
    setLeaving(chosen);
    // Animasyon bitince seçimi uygula.
    window.setTimeout(() => {
      setLeaving(null);
      setDx(0);
      past.current = false;
      onChoose(chosen);
    }, calm() ? 0 : 200);
  };

  // Klavyedeki ok tuşları.
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "ArrowLeft") fling("left");
      if (event.key === "ArrowRight") fling("right");
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  // Kartın anlık dönüşümü.
  const transform = leaving
    ? `translateX(${(leaving === "right" ? 1 : -1) * (window.innerWidth + 150)}px) rotate(${leaving === "right" ? 22 : -22}deg)`
    : `translateX(${dx}px) rotate(${dx / 18}deg)`;

  return (
    <div className="app" data-danger={lowest <= 1 ? "high" : lowest === 2 ? "low" : "none"}>
      {/* Sarsıntı katmanı: key değişince animasyon baştan başlar. */}
      <div className="play" key={hit.count} data-shake={hit.count > 0}>
      <div className="top">
        {/* key saat değişince animasyonu yeniden başlatır. */}
        <div className="time" key={card.time}>
          {card.time}
        </div>
        <Controls language={language} onLanguage={onLanguage} />
      </div>
      {/* Günün ilerlemesi: Hedef gece yarısı. */}
      <div className="day" aria-label={UI.goal[language]}>
        <div className="day-fill" style={{ width: `${dayProgress(card.time) * 100}%` }} />
        <span className="day-label">
          <span>{UI.goal[language]}</span>
          <span>
            {UI.attempt[language]} {tries}
          </span>
        </span>
      </div>
      {/* Göstergeler. */}
      <div className="meters">
        {story.meters.map((meter) => (
          <div className="meter" key={meter.id} data-hint={affected.includes(meter.id)} data-low={state.meters[meter.id] <= 1} data-hit={hit.ids.includes(meter.id)}>
            <span className="meter-name">
              {meter.emoji} {say(meter.label, language)}
            </span>
            <span className="pips" role="img" aria-label={`${state.meters[meter.id]} / ${story.maxMeter}`}>
              {Array.from({ length: story.maxMeter }, (_, i) => (
                <i className="pip" key={i} data-on={i < state.meters[meter.id]} />
              ))}
            </span>
          </div>
        ))}
      </div>
      {/* Önceki seçimin sonucu. */}
      <p className="after" key={`after-${card.id}`} aria-live="polite">
        {state.after ? say(state.after, language) : ""}
      </p>
      {/* Kart masası. */}
      <div className="table">
        <div className="card back" aria-hidden="true" />
        <div
          className="card live"
          key={card.id}
          data-moving={dragging.current}
          style={{ transform }}
          onPointerDown={(event) => {
            // Uçarken dokunma yok.
            if (leaving) return;
            dragging.current = true;
            startX.current = event.clientX;
            unlockAudio();
            event.currentTarget.setPointerCapture?.(event.pointerId);
          }}
          onPointerMove={(event) => {
            // Parmak kartta değilse geç.
            if (!dragging.current) return;
            const moved = event.clientX - startX.current;
            setDx(moved);
            // Eşik geçilince tık.
            const over = Math.abs(moved) > threshold;
            if (over && !past.current) {
              sfx.snap();
              vibrate(8);
            }
            past.current = over;
          }}
          onPointerUp={() => {
            // Parmak kalktı.
            if (!dragging.current) return;
            dragging.current = false;
            if (Math.abs(dx) > threshold) fling(dx > 0 ? "right" : "left");
            else setDx(0);
          }}
          onPointerCancel={() => {
            dragging.current = false;
            setDx(0);
          }}
        >
          <div className="who">{say(card.who, language)}</div>
          <div className="face" aria-hidden="true">
            {card.emoji}
          </div>
          <p className="text">{say(cardText(card, state.flags), language)}</p>
          {/* Sürüklerken seçimin yazısı. Kart sola kayarken sağ kenarı ekranda kalır, bu yüzden yazı ters kenarda. */}
          <div className="tag left" style={{ opacity: dx < 0 ? Math.min(1, -dx / 60) : 0 }}>
            {say(card.left.label, language)}
          </div>
          <div className="tag right" style={{ opacity: dx > 0 ? Math.min(1, dx / 60) : 0 }}>
            {say(card.right.label, language)}
          </div>
        </div>
      </div>
      {/* Düğmeler: Kaydırmak istemeyenler için. */}
      <div className="choices">
        <button className="pick l" onClick={() => fling("left")}>
          ← {say(card.left.label, language)}
        </button>
        <button className="pick r" onClick={() => fling("right")}>
          {say(card.right.label, language)} →
        </button>
      </div>
      </div>
    </div>
  );
}

// Ölüm ekranı: Zinciri adım adım gösterir.
export function DeathScreen({ story, language, onLanguage, ending, state, stats, found, onRewind, onDeaths }: Common & { ending: Ending; state: GameState; stats: RunStats | null; found: number; onRewind: () => void; onDeaths: () => void }) {
  // Bu koşuda geçerli zincir adımları.
  const steps = ending.chain.filter((step) => holds(step.when, state.flags));
  // Kaç adım göründü.
  const [shown, setShown] = useState(0);

  // Ölüm sesi ve zincir.
  useEffect(() => {
    // Darbe.
    sfx.death();
    vibrate([120, 60, 260]);
    // Uğultu ve kalp durur: Ölüm.
    setHeartbeat(0);
    setDrone(0.02);
    // Adımları tek tek göster.
    const timers = steps.map((_, i) =>
      window.setTimeout(() => {
        setShown(i + 1);
        sfx.chain();
      }, 900 + i * 520),
    );
    return () => timers.forEach((timer) => window.clearTimeout(timer));
    // Sadece ekran açılınca çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Tüm adımlar göründü mü?
  const finished = shown >= steps.length;
  // Toplam ölüm sayısı.
  const total = story.endings.filter((item) => !item.won).length;
  // Veri azken yüzde yanıltıcı olur, 20 koşudan sonra gösteriyoruz.
  const share = stats && stats.runs >= 20 ? Math.round((100 * stats.sameEnding) / stats.runs) : null;

  return (
    <div className="app dead-screen">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="screen">
        <h1 className="big strike">{say(ending.title, language)}</h1>
        <p className="lede">{say(ending.text, language)}</p>
        <div>
          <p className="chain-label">{UI.chain[language]}</p>
          <ol className="chain">
            {steps.map((step, i) => (
              <li key={i} data-on={i < shown}>
                {say(step.text, language)}
              </li>
            ))}
          </ol>
        </div>
        {/* İpucu en sonda gelir: "demek öyleymiş" anı. */}
        <p className="could" data-on={finished}>
          {say(ending.hint, language)}
        </p>
        {share !== null && <p className="small">{UI.shareOfPlayers[language].replace("{percent}", String(share))}</p>}
        <button className="go" onClick={onRewind}>
          {UI.rewind[language]}
        </button>
        <button className="link" onClick={onDeaths}>
          {UI.deaths[language]} {found}/{total}
        </button>
      </div>
    </div>
  );
}

// Kurtuluş ekranı.
export function WinScreen({ language, onLanguage, ending, tries, onAgain }: Common & { ending: Ending; tries: number; onAgain: () => void }) {
  // Akor ve sessizlik.
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

// Zamanın geri sarılması: Saat hızla geriye akar, sonra oyun başlar.
export function RewindScreen({ from, to, onDone }: { from: string; to: string; onDone: () => void }) {
  // Dakika cinsine çevirir.
  const minutes = (time: string) => {
    const [hours, mins] = time.split(":").map(Number);
    return hours * 60 + mins;
  };
  // Ekranda görünen dakika.
  const [now, setNow] = useState(minutes(from));

  useEffect(() => {
    // Hareket azaltma açıksa hemen geç.
    if (calm()) {
      onDone();
      return;
    }
    // Geri sarma sesi.
    sfx.rewind();
    setDrone(0.06);
    // Başlangıç ve süre.
    const started = performance.now();
    const duration = 1100;
    const start = minutes(from);
    const end = minutes(to);
    // Her karede saati güncelle.
    let frame = requestAnimationFrame(function update(time) {
      const progress = Math.min(1, (time - started) / duration);
      setNow(Math.round(start + (end - start) * progress * progress));
      if (progress < 1) frame = requestAnimationFrame(update);
      else onDone();
    });
    return () => cancelAnimationFrame(frame);
    // Sadece ekran açılınca çalışsın.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Dakikayı 08:14 biçimine çevir.
  const label = `${String(Math.floor(now / 60)).padStart(2, "0")}:${String(now % 60).padStart(2, "0")}`;
  return (
    <div className="rewind">
      <div className="rewind-clock">{label}</div>
    </div>
  );
}

// Bulunan ölümler listesi.
export function DeathsScreen({ story, language, onLanguage, found, onBack }: Common & { found: string[]; onBack: () => void }) {
  // Sadece ölümler.
  const deaths = story.endings.filter((ending) => !ending.won);
  return (
    <div className="app">
      <Controls language={language} onLanguage={onLanguage} />
      <div className="screen list-screen">
        <h1 className="big small-big">{UI.deaths[language]}</h1>
        <p className="small">
          {UI.found[language]}: {deaths.filter((ending) => found.includes(ending.id)).length}/{deaths.length}
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
