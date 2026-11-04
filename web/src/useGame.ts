// React'in durum ve yan etki araçları.
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
// Sunucu çağrıları.
import { fetchTodaysScene, fetchTodaysStats, playAttempt } from "./api";
// Tipler.
import type { Attempt, GameEvent, PublicScene, SceneStats, Touch } from "./types";

// Oyunun aşamaları: yükleniyor, hazır, kehanet, uyanış, oyun, kayıp, zafer, hata.
export type Phase = "loading" | "ready" | "vision" | "awake" | "playing" | "lost" | "won" | "error";

// Kehanette olay olmayan bir saniyenin süresi: boş anlar hızlı geçer.
const VISION_EMPTY_MS = 350;
// Kehanette cümlesi olan bir saniyenin süresi: oyuncu okuyabilsin.
const VISION_READ_MS = 2400;
// Kehanette felaket saniyesinin süresi: en önemli an, biraz daha uzun kalır.
const VISION_DISASTER_MS = 3000;
// Kehanet ile gerçek arasındaki bekleme.
const AWAKE_MS = 1500;
// Etkisiz dokunuş uyarısının ekranda kalma süresi.
const NOTHING_MS = 900;

// Ekranda nesnenin üstünde gösterilecek baloncuk.
export type Bubble = { object: string; text: string; danger: boolean };

// Tüm oyun mantığı bu hook'ta. Ekran kodu ile karışmasın diye ayrı dosyada.
export function useGame() {
  // Sahne.
  const [scene, setScene] = useState<PublicScene | null>(null);
  // Oyunun aşaması.
  const [phase, setPhase] = useState<Phase>("loading");
  // Şu anki saniye.
  const [second, setSecond] = useState(0);
  // Bu koşuda yapılan dokunuşlar. Her koşu sıfırdan başlar.
  const [touches, setTouches] = useState<Touch[]>([]);
  // Sunucunun bu koşu için döndürdüğü sonuç.
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  // Kaçıncı koşu.
  const [attemptCount, setAttemptCount] = useState(0);
  // Az önce etkisiz dokunulan nesne. O nesne sallanır.
  const [nothingOn, setNothingOn] = useState<string | null>(null);
  // Kazanınca gösterilen istatistikler.
  const [stats, setStats] = useState<SceneStats | null>(null);
  // Sunucu cevabı beklenirken ikinci dokunuşu engellemek için.
  const waitingForServer = useRef(false);
  // Bu koşunun kimliği. Aynı koşunun tüm istekleri tek satır olarak kaydedilir.
  const attemptId = useRef("");

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

  // Kazanınca istatistikleri getir. Gelmezse oyun yine çalışır, sadece gösterilmez.
  useEffect(() => {
    // Sadece kazanınca.
    if (phase !== "won") return;
    // İste ve kaydet.
    fetchTodaysStats()
      .then(setStats)
      .catch(() => setStats(null));
  }, [phase]);

  // Yeni bir gerçek koşu başlatır. Dokunuşlar sıfırlanır.
  const startRun = useCallback(async () => {
    // Sahne yoksa başlatma.
    if (!scene) return;
    try {
      // Yeni koşu, yeni kimlik.
      attemptId.current = crypto.randomUUID();
      // Sunucudan dokunuşsuz sonucu al.
      const result = await playAttempt(scene.id, [], attemptId.current);
      // Dokunuşları sıfırla.
      setTouches([]);
      // Sonucu kaydet.
      setAttempt(result);
      // Zamanı başa sar.
      setSecond(0);
      // Koşu sayısını artır.
      setAttemptCount((count) => count + 1);
      // Gerçeği başlat.
      setPhase("playing");
    } catch {
      // Sunucuya ulaşılamadı.
      setPhase("error");
    }
  }, [scene]);

  // Kehaneti baştan oynatır.
  const startVision = useCallback(() => {
    // Zamanı başa sar.
    setSecond(0);
    // Kehanete geç.
    setPhase("vision");
  }, []);

  // Kehaneti atlar ve gerçeğe geçer.
  const skipVision = useCallback(() => {
    // Sayaç başa döner.
    setSecond(0);
    // Uyanışa geç.
    setPhase("awake");
  }, []);

  // Zamanı ilerleten kısım.
  useEffect(() => {
    // Kehanet aşaması.
    if (phase === "vision" && scene) {
      // Süre bittiyse uyanışa geç.
      if (second >= scene.durationSeconds) {
        const timer = setTimeout(() => {
          // Sayaç başa döner.
          setSecond(0);
          // Uyanışa geç.
          setPhase("awake");
        }, VISION_READ_MS);
        return () => clearTimeout(timer);
      }
      // Bu saniyede felaket var mı?
      const hasDisaster = scene.vision.disasters.some((disaster) => disaster.second === second);
      // Bu saniyede okunacak bir cümle var mı?
      const hasCaption = scene.vision.events.some((event) => event.second === second && event.caption);
      // Saniyenin ekranda kalma süresini seç.
      const delay = hasDisaster ? VISION_DISASTER_MS : hasCaption ? VISION_READ_MS : VISION_EMPTY_MS;
      // Süre dolunca ilerle.
      const timer = setTimeout(() => setSecond((current) => current + 1), delay);
      return () => clearTimeout(timer);
    }

    // Uyanış aşaması: kısa bir bekleyişten sonra gerçek başlar.
    if (phase === "awake") {
      const timer = setTimeout(startRun, AWAKE_MS);
      return () => clearTimeout(timer);
    }

    // Gerçek oyun aşaması.
    if (phase === "playing" && attempt) {
      // Koşunun bitiş saniyesine geldik mi?
      if (second >= attempt.endSecond) {
        // Sonucu ekrana yansıt.
        setPhase(attempt.won ? "won" : "lost");
        return;
      }
      // Bir saniye sonra saniyeyi artır.
      const timer = setTimeout(() => setSecond((current) => current + 1), 1000);
      // Bu kod tekrar çalışmadan önce eski sayacı iptal et.
      return () => clearTimeout(timer);
    }
  }, [phase, second, attempt, scene, startRun]);

  // Oyuncu bir nesneye dokunduğunda çalışır.
  const touchObject = useCallback(
    async (objectId: string) => {
      // Sadece oyun sırasında ve hakkı varsa dokunulabilir.
      if (!scene || phase !== "playing" || waitingForServer.current) return;
      // Dokunuş hakkı bitti mi?
      if (touches.length >= scene.maxTouches) return;
      // Aynı saniyede aynı nesneye tekrar dokunmanın anlamı yok.
      if (touches.some((touch) => touch.object === objectId && touch.second === second)) return;

      // Eski dokunuşlara yenisini ekle.
      const nextTouches = [...touches, { object: objectId, second }];
      // Cevap gelene kadar yeni dokunuşu engelle.
      waitingForServer.current = true;
      try {
        // Sunucudan yeni sonucu al. Geçmiş değişmediği için oyun kaldığı yerden devam eder.
        const result = await playAttempt(scene.id, nextTouches, attemptId.current);
        // Bu dokunuşun etkisi oldu mu?
        const wasted = result.events.some(
          (event) => event.noEffect && event.object === objectId && event.second === second,
        );
        // Etkisizse hak yemez: nesneyi salla ve eski sonucu koru.
        if (wasted) {
          setNothingOn(objectId);
          setTimeout(() => setNothingOn(null), NOTHING_MS);
          return;
        }
        // Dokunuşları güncelle.
        setTouches(nextTouches);
        // Sonucu güncelle.
        setAttempt(result);
      } catch {
        // Sunucuya ulaşılamadı.
        setPhase("error");
      } finally {
        // Hata olsa da olmasa da engeli kaldır.
        waitingForServer.current = false;
      }
    },
    [scene, phase, touches, second],
  );

  // Şu an ekranda geçerli olay listesi: kehanette kehanetin, oyunda koşunun olayları.
  const events: GameEvent[] = useMemo(() => {
    // Kehanette kehanetin olayları.
    if (phase === "vision") return scene?.vision.events ?? [];
    // Uyanışta sahne başlangıç haline döner.
    if (phase === "awake" || phase === "ready") return [];
    // Diğer durumlarda koşunun olayları.
    return attempt?.events ?? [];
  }, [phase, scene, attempt]);

  // Nesnelerin şu anki emojileri. Saklamak yerine olay listesinden hesaplıyoruz, böylece hiç tutarsız olamaz.
  const emojis = useMemo(() => {
    // Sonuç tablosu.
    const current: Record<string, string> = {};
    // Başlangıç emojilerini yaz.
    scene?.objects.forEach((object) => (current[object.id] = object.emoji));
    // Şu ana kadar olmuş olayları uygula.
    events
      .filter((event) => event.second <= second)
      .forEach((event) => (current[event.object] = event.emoji));
    // Tabloyu döndür.
    return current;
  }, [scene, events, second]);

  // Nesnenin üstünde gösterilecek baloncuk.
  const bubble: Bubble | null = useMemo(() => {
    // Etkisiz dokunuş uyarısı en önceliklisi.
    if (nothingOn) return { object: nothingOn, text: "Nothing happens.", danger: false };

    // Şu ana kadarki, cümlesi olan son olay.
    const latest = events.filter((event) => event.second <= second && event.caption).at(-1);

    // Kayıpta felaket cümlesi son olayın nesnesinde çıkar.
    if (phase === "lost" && latest && attempt?.disasterCaption) {
      return { object: latest.object, text: attempt.disasterCaption, danger: true };
    }

    // Kehanette felaket saniyesine gelindiyse felaket cümlesi çıkar.
    const disaster =
      phase === "vision" ? scene?.vision.disasters.filter((item) => item.second <= second).at(-1) : undefined;
    // Felaket son olaydan yeni ya da aynı andaysa onu göster.
    if (latest && disaster && disaster.second >= latest.second) {
      return { object: latest.object, text: disaster.caption, danger: true };
    }

    // Olay yoksa baloncuk da yok.
    if (!latest?.caption) return null;
    // Normal olay cümlesi.
    return { object: latest.object, text: latest.caption, danger: false };
  }, [nothingOn, events, second, phase, attempt, scene]);

  // Ekrana gereken her şeyi döndür.
  return {
    scene,
    phase,
    second,
    touches,
    attempt,
    attemptCount,
    nothingOn,
    stats,
    emojis,
    bubble,
    startVision,
    skipVision,
    startRun,
    touchObject,
  };
}
