// React'in durum ve yan etki araçları.
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
// Sunucu çağrıları.
import { fetchTodaysScene, playAttempt } from "./api";
// Tipler.
import type { Attempt, PublicScene, Touch } from "./types";

// Oyunun hangi aşamada olduğu. Swift'teki enum gibi.
export type Phase = "loading" | "ready" | "playing" | "lost" | "won" | "error";

// Tüm oyun mantığı bu hook'ta. Ekran kodu ile karışmasın diye ayrı dosyada.
export function useGame() {
  // Sahne.
  const [scene, setScene] = useState<PublicScene | null>(null);
  // Oyunun aşaması.
  const [phase, setPhase] = useState<Phase>("loading");
  // Şu anki saniye.
  const [second, setSecond] = useState(0);
  // Biriken dokunuşlar. Her döngüde tekrar oynanır.
  const [touches, setTouches] = useState<Touch[]>([]);
  // Sunucunun bu döngü için döndürdüğü sonuç.
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  // Kaçıncı deneme.
  const [attemptCount, setAttemptCount] = useState(0);
  // Bu döngüde yeni dokunuş yapıldı mı?
  const [touchedThisLoop, setTouchedThisLoop] = useState(false);
  // Sunucu cevabı beklenirken ikinci dokunuşu engellemek için.
  const waitingForServer = useRef(false);

  // Sayfa açılınca sahneyi bir kere yükle.
  useEffect(() => {
    // Sahneyi iste.
    fetchTodaysScene()
      // Gelince kaydet ve hazır ol.
      .then((loaded) => {
        setScene(loaded);
        setPhase("ready");
      })
      // Sunucuya ulaşılamazsa hata ekranına geç.
      .catch(() => setPhase("error"));
  }, []);

  // Zamanı ilerleten kısım. Saniye ya da sonuç değişince yeniden çalışır.
  useEffect(() => {
    // Sadece oyun sırasında ilerle.
    if (phase !== "playing" || !attempt) return;
    // Döngünün bitiş saniyesine geldik mi?
    if (second >= attempt.endSecond) {
      // Sonucu ekrana yansıt.
      setPhase(attempt.won ? "won" : "lost");
      return;
    }
    // Bir saniye sonra saniyeyi artır.
    const timer = setTimeout(() => setSecond((current) => current + 1), 1000);
    // Bu kod tekrar çalışmadan önce eski sayacı iptal et.
    return () => clearTimeout(timer);
  }, [phase, second, attempt]);

  // Verilen dokunuşlarla yeni bir döngü başlatır.
  const startLoop = useCallback(
    async (loopTouches: Touch[]) => {
      // Sahne yoksa başlatma.
      if (!scene) return;
      try {
        // Sunucudan bu dokunuşların sonucunu al.
        const result = await playAttempt(scene.id, loopTouches);
        // Dokunuşları kaydet.
        setTouches(loopTouches);
        // Sonucu kaydet.
        setAttempt(result);
        // Zamanı başa sar.
        setSecond(0);
        // Bu döngüde henüz dokunulmadı.
        setTouchedThisLoop(false);
        // Deneme sayısını artır.
        setAttemptCount((count) => count + 1);
        // Oyunu başlat.
        setPhase("playing");
      } catch {
        // Sunucuya ulaşılamadı.
        setPhase("error");
      }
    },
    [scene],
  );

  // Oyuncu bir nesneye dokunduğunda çalışır.
  const touchObject = useCallback(
    async (objectId: string) => {
      // Sadece oyun sırasında, döngü başına bir kez ve hakkı varsa dokunulabilir.
      if (!scene || phase !== "playing" || touchedThisLoop || waitingForServer.current) return;
      // Dokunuş hakkı bitti mi?
      if (touches.length >= scene.maxTouches) return;
      // Aynı saniyede aynı nesneye tekrar dokunulamaz.
      if (touches.some((touch) => touch.object === objectId && touch.second === second)) return;

      // Eski dokunuşlara yenisini ekle.
      const nextTouches = [...touches, { object: objectId, second }];
      // Cevap gelene kadar yeni dokunuşu engelle.
      waitingForServer.current = true;
      try {
        // Sunucudan yeni sonucu al. Geçmiş değişmediği için oyun kaldığı yerden devam eder.
        const result = await playAttempt(scene.id, nextTouches);
        // Dokunuşları güncelle.
        setTouches(nextTouches);
        // Sonucu güncelle.
        setAttempt(result);
        // Bu döngünün dokunuş hakkı kullanıldı.
        setTouchedThisLoop(true);
      } catch {
        // Sunucuya ulaşılamadı.
        setPhase("error");
      } finally {
        // Hata olsa da olmasa da engeli kaldır.
        waitingForServer.current = false;
      }
    },
    [scene, phase, touchedThisLoop, touches, second],
  );

  // Kaybettikten sonra tekrar dener. Dokunuş hakkı dolduysa son dokunuşu geri alır.
  const rewind = useCallback(() => {
    // Sahne yoksa çık.
    if (!scene) return;
    // Hak dolduysa son dokunuşu çıkar, yoksa hepsini koru.
    const kept = touches.length >= scene.maxTouches ? touches.slice(0, -1) : touches;
    // Yeni döngüyü başlat.
    startLoop(kept);
  }, [scene, touches, startLoop]);

  // Tüm dokunuşları silip baştan başlatır.
  const startOver = useCallback(() => startLoop([]), [startLoop]);

  // Nesnelerin şu anki emojileri. Saklamak yerine olay listesinden hesaplıyoruz, böylece hiç tutarsız olamaz.
  const emojis = useMemo(() => {
    // Sonuç tablosu.
    const current: Record<string, string> = {};
    // Başlangıç emojilerini yaz.
    scene?.objects.forEach((object) => (current[object.id] = object.emoji));
    // Şu ana kadar olmuş olayları uygula.
    attempt?.events
      .filter((event) => event.second <= second)
      .forEach((event) => (current[event.object] = event.emoji));
    // Tabloyu döndür.
    return current;
  }, [scene, attempt, second]);

  // Ekranda gösterilecek son açıklama.
  const caption = useMemo(() => {
    // Olmuş ve açıklaması olan olayları al.
    const shown = attempt?.events.filter((event) => event.second <= second && event.caption);
    // Sonuncusunun açıklamasını döndür.
    return shown?.at(-1)?.caption ?? null;
  }, [attempt, second]);

  // Ekrana gereken her şeyi döndür.
  return {
    scene,
    phase,
    second,
    touches,
    attempt,
    attemptCount,
    touchedThisLoop,
    emojis,
    caption,
    startLoop,
    touchObject,
    rewind,
    startOver,
  };
}