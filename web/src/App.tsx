// React araçları.
import { useCallback, useEffect, useRef, useState } from "react";
// Sunucu çağrıları.
import { fetchStory, newId, submitRun } from "./api";
// Motor.
import { choose, newGame, say } from "./engine";
// Yazılar ve dil.
import { UI, initialLanguage, saveLanguage } from "./i18n";
// Ekranlar.
import { DeathScreen, DeathsScreen, PlayScreen, RewindScreen, TitleScreen, VisionScreen, WinScreen } from "./Screens";
// Tipler.
import type { GameState, Language, RunStats, Side, Story } from "./types";

// Oyunun ekranları.
type Screen = "loading" | "error" | "title" | "vision" | "play" | "end" | "rewind" | "deaths";

// Bulunan sonların saklandığı anahtar.
const FOUND_KEY = "until-midnight.found";

// Saklı sonları okur.
function readFound(): string[] {
  try {
    // Yoksa boş liste.
    return JSON.parse(localStorage.getItem(FOUND_KEY) ?? "[]");
  } catch {
    // Depo yoksa boş liste.
    return [];
  }
}

// Ana bileşen: Hangi ekranın görüneceğine karar verir.
export function App() {
  // Hikâye.
  const [story, setStory] = useState<Story | null>(null);
  // Görünen ekran.
  const [screen, setScreen] = useState<Screen>("loading");
  // Dil.
  const [language, setLanguageState] = useState<Language>(initialLanguage);
  // Oyunun anlık hali.
  const [state, setState] = useState<GameState | null>(null);
  // Kaçıncı deneme.
  const [tries, setTries] = useState(0);
  // Bulunan sonlar.
  const [found, setFound] = useState<string[]>(readFound);
  // Sunucudan gelen istatistikler.
  const [stats, setStats] = useState<RunStats | null>(null);
  // Ölüm listesinden dönülecek ekran.
  const returnTo = useRef<Screen>("title");
  // Geri sarmanın başladığı saat.
  const rewindFrom = useRef("23:40");

  // Sayfanın dil işaretini güncel tut.
  useEffect(() => {
    document.documentElement.lang = language;
    // Sekme başlığı da seçili dilde olsun.
    if (story) document.title = say(story.title, language);
  }, [language, story]);

  // Dili değiştirir: Tüm metinler bu değerden okunduğu için anında değişir.
  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next);
    saveLanguage(next);
  }, []);

  // Hikâyeyi bir kere yükle.
  useEffect(() => {
    fetchStory()
      .then((loaded) => {
        setStory(loaded);
        setScreen("title");
      })
      .catch(() => setScreen("error"));
  }, []);

  // Yeni koşu başlatır.
  const startRun = useCallback(() => {
    if (!story) return;
    setState(newGame(story));
    setStats(null);
    setTries((count) => count + 1);
    setScreen("play");
  }, [story]);

  // Seçimi uygular.
  const onChoose = useCallback(
    (side: Side) => {
      if (!story || !state) return;
      const next = choose(story, state, side);
      setState(next);
      // Oyun sürüyorsa devam.
      if (!next.endingId) return;
      // Bulunan sonlara ekle.
      setFound((current) => {
        if (current.includes(next.endingId!)) return current;
        const updated = [...current, next.endingId!];
        try {
          localStorage.setItem(FOUND_KEY, JSON.stringify(updated));
        } catch {
          // Saklanamazsa sadece bu oturumda hatırlanır.
        }
        return updated;
      });
      // Sunucuya bildir. Hata olursa oyun yine sürer, sadece istatistik gelmez.
      submitRun(story.id, newId(), next.choices)
        .then(setStats)
        .catch(() => setStats(null));
      // Geri sarmanın başlayacağı saat.
      rewindFrom.current = story.cards[next.index].time;
      setScreen("end");
    },
    [story, state],
  );

  // Hikâye gelmeden gösterilen ekranlar.
  if (screen === "error") {
    return (
      <div className="app">
        <div className="screen">
          <p className="lede">{UI.error[language]}</p>
        </div>
      </div>
    );
  }
  if (!story) {
    return (
      <div className="app">
        <div className="screen">
          <p className="lede">{UI.loading[language]}</p>
        </div>
      </div>
    );
  }

  // Ortak özellikler.
  const common = { story, language, onLanguage: setLanguage };
  // Bulunan ölüm sayısı.
  const foundDeaths = story.endings.filter((ending) => !ending.won && found.includes(ending.id)).length;
  // Ölüm listesini açar.
  const openDeaths = (from: Screen) => {
    returnTo.current = from;
    setScreen("deaths");
  };

  // Ekranı seç.
  switch (screen) {
    case "vision":
      return <VisionScreen story={story} language={language} onDone={startRun} />;
    case "play":
      return <PlayScreen {...common} state={state!} tries={tries} onChoose={onChoose} />;
    case "rewind":
      return <RewindScreen from={rewindFrom.current} to={story.cards[0].time} onDone={startRun} />;
    case "deaths":
      return <DeathsScreen {...common} found={found} onBack={() => setScreen(returnTo.current)} />;
    case "end": {
      // Ulaşılan son.
      const ending = story.endings.find((item) => item.id === state!.endingId)!;
      // Kurtuluş mu ölüm mü?
      return ending.won ? (
        <WinScreen {...common} ending={ending} tries={tries} onAgain={() => setScreen("rewind")} />
      ) : (
        <DeathScreen
          {...common}
          ending={ending}
          state={state!}
          stats={stats}
          found={foundDeaths}
          onRewind={() => setScreen("rewind")}
          onDeaths={() => openDeaths("end")}
        />
      );
    }
    default:
      return <TitleScreen {...common} found={foundDeaths} onStart={() => setScreen("vision")} onDeaths={() => openDeaths("title")} />;
  }
}
