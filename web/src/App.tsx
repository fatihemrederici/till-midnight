// React araçları.
import { useCallback, useEffect, useRef, useState } from "react";
// Sunucu çağrıları.
import { fetchChapters, fetchStory, submitRun } from "./data/api";
// Motor.
import { choose, newGame } from "./game/engine";
// Tipler.
import type { GameState, Language, RunStats, Side, Story, StorySummary } from "./game/types";
// Bulunan sonlar.
import { useFoundEndings } from "./hooks/useFoundEndings";
// Dil.
import { initialLanguage, saveLanguage } from "./i18n/languages";
// Yazılar.
import { UI } from "./i18n/ui";
// Ekranlar.
import { DeathScreen } from "./screens/DeathScreen";
import { DeathsScreen } from "./screens/DeathsScreen";
import { MessageScreen } from "./screens/MessageScreen";
import { PlayScreen } from "./screens/PlayScreen";
import { RewindScreen } from "./screens/RewindScreen";
import { TitleScreen } from "./screens/TitleScreen";
import { VisionScreen } from "./screens/VisionScreen";
import { WinScreen } from "./screens/WinScreen";

// Oyunun ekranları.
type Screen = "loading" | "error" | "title" | "vision" | "play" | "end" | "rewind" | "deaths";

// Ana bileşen: Hangi ekranın görüneceğine karar verir ve oyun durumunu tutar.
export function App() {
  // Bölüm listesi.
  const [chapters, setChapters] = useState<StorySummary[]>([]);
  // Seçilen bölüm.
  const [story, setStory] = useState<Story | null>(null);
  // Görünen ekran.
  const [screen, setScreen] = useState<Screen>("loading");
  // Dil.
  const [language, setLanguageState] = useState<Language>(initialLanguage);
  // Oyunun anlık hali.
  const [state, setState] = useState<GameState | null>(null);
  // Kaçıncı deneme.
  const [tries, setTries] = useState(0);
  // Sunucudan gelen istatistikler.
  const [stats, setStats] = useState<RunStats | null>(null);
  // Bulunan sonlar.
  const { found, addFound } = useFoundEndings(story?.id ?? "");
  // Ölüm listesinden dönülecek ekran.
  const returnTo = useRef<Screen>("end");
  // Geri sarmanın başladığı saat.
  const rewindFrom = useRef("23:40");

  // Sayfanın dilini ve sekme başlığını güncel tut.
  useEffect(() => {
    document.documentElement.lang = language;
    document.title = UI.gameTitle[language];
  }, [language]);

  // Dili değiştirir: Tüm metinler bu değerden okunduğu için anında değişir.
  const setLanguage = useCallback((next: Language) => {
    setLanguageState(next);
    saveLanguage(next);
  }, []);

  // Bölüm listesini bir kere yükle.
  useEffect(() => {
    fetchChapters()
      .then((loaded) => {
        setChapters(loaded);
        setScreen("title");
      })
      .catch(() => setScreen("error"));
  }, []);

  // Seçilen bölümü yükleyip kehaneti başlatır.
  const pickChapter = useCallback((id: string) => {
    setScreen("loading");
    fetchStory(id)
      .then((loaded) => {
        setStory(loaded);
        setTries(0);
        setScreen("vision");
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
      addFound(next.endingId);
      // Sunucuya bildir. Hata olursa oyun yine sürer, sadece istatistik gelmez.
      submitRun(story, next.choices)
        .then(setStats)
        .catch(() => setStats(null));
      // Geri sarmanın başlayacağı saat.
      rewindFrom.current = story.cards[next.index].time;
      setScreen("end");
    },
    [story, state, addFound],
  );

  // Hikâye gelmeden gösterilen ekranlar.
  if (screen === "error") return <MessageScreen message={UI.error[language]} />;
  if (screen === "loading") return <MessageScreen message={UI.loading[language]} />;
  if (screen === "title" || !story) {
    return (
      <TitleScreen
        chapters={chapters}
        language={language}
        onLanguage={setLanguage}
        onPick={pickChapter}
      />
    );
  }

  // Ortak özellikler.
  const common = { story, language, onLanguage: setLanguage };
  // Bulunan ölüm sayısı.
  const foundDeaths = story.endings.filter((item) => !item.won && found.includes(item.id)).length;
  // Ölüm listesini açar.
  const openDeaths = () => {
    returnTo.current = "end";
    setScreen("deaths");
  };
  // Bölüm listesine döner.
  const openChapters = () => setScreen("title");

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
        <WinScreen
          language={language}
          onLanguage={setLanguage}
          ending={ending}
          tries={tries}
          onAgain={() => setScreen("rewind")}
          onChapters={openChapters}
        />
      ) : (
        <DeathScreen
          {...common}
          ending={ending}
          state={state!}
          stats={stats}
          found={foundDeaths}
          onRewind={() => setScreen("rewind")}
          onDeaths={openDeaths}
          onChapters={openChapters}
        />
      );
    }
    default:
      return null;
  }
}
