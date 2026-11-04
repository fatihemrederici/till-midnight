// Tipleri içeri alıyoruz.
import type { Attempt, Level, PublicScene, SceneStats, Touch } from "./types";

// Oyuncunun kimliğini tarayıcıda saklarken kullanılan anahtar.
const PLAYER_KEY = "10sec.playerId";

// Oyuncu kimliğini döndürür. Hesap yok: ilk ziyarette rastgele üretilir ve tarayıcıda saklanır.
function playerId(): string {
  // Bazı gizli modlarda tarayıcı deposu hata verebilir, bu yüzden try kullanıyoruz.
  try {
    // Saklı kimlik varsa onu kullan.
    const saved = localStorage.getItem(PLAYER_KEY);
    if (saved) return saved;
    // Yoksa yeni üret.
    const created = crypto.randomUUID();
    // Sakla.
    localStorage.setItem(PLAYER_KEY, created);
    // Döndür.
    return created;
  } catch {
    // Depo kullanılamıyorsa bu sayfa için geçici kimlik üret.
    return crypto.randomUUID();
  }
}

// Oyuncunun bölüm listesini sunucudan alır. async: bekleyen işlem. Swift'teki async gibi.
export async function fetchLevels(): Promise<Level[]> {
  // İsteği gönder ve cevabı bekle.
  const response = await fetch(`/levels?playerId=${playerId()}`);
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // Gelen JSON'u nesneye çevir.
  return response.json();
}

// Bir bölümün sahnesini sunucudan alır.
export async function fetchScene(sceneId: string): Promise<PublicScene> {
  // İsteği gönder ve cevabı bekle.
  const response = await fetch(`/scene/${sceneId}`);
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // Gelen JSON'u nesneye çevir.
  return response.json();
}

// Dokunuşları sunucuya gönderir, olacakları alır.
export async function playAttempt(sceneId: string, touches: Touch[], attemptId: string): Promise<Attempt> {
  // POST isteği gönder.
  const response = await fetch("/attempt", {
    method: "POST",
    // Gövdenin JSON olduğunu söyle.
    headers: { "Content-Type": "application/json" },
    // Nesneyi JSON metnine çevir.
    body: JSON.stringify({ sceneId, touches, attemptId, playerId: playerId() }),
  });
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // Gelen JSON'u nesneye çevir.
  return response.json();
}

// Bir bölümün bugünkü istatistiklerini sunucudan alır.
export async function fetchStats(sceneId: string): Promise<SceneStats> {
  // İsteği gönder ve cevabı bekle.
  const response = await fetch(`/stats/${sceneId}`);
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // Gelen JSON'u nesneye çevir.
  return response.json();
}
