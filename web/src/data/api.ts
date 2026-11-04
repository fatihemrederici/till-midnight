// Tipler.
import type { RunStats, Side, Story } from "../game/types";
// Tarayıcı deposu.
import { readValue, writeValue } from "./storage";

// Rastgele kimlik üretir. Telefondan http ile açınca crypto.randomUUID olmayabilir, bu yüzden yedeği var.
export function newId(): string {
  // Varsa hazır fonksiyonu kullan.
  if (crypto.randomUUID) return crypto.randomUUID();
  // Yoksa 16 rastgele bayttan UUID oluştur.
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  // UUID sürüm ve varyant bitleri.
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  // Baytları onaltılık metne çevir.
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
  // Tireleri ekle.
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

// Oyuncu kimliği: İlk ziyarette üretilir ve saklanır.
function playerId(): string {
  // Saklı kimlik.
  const saved = readValue("playerId");
  if (saved) return saved;
  // Yoksa yeni üret ve sakla.
  const created = newId();
  writeValue("playerId", created);
  return created;
}

// Hikâyeyi sunucudan alır.
export async function fetchStory(): Promise<Story> {
  // İsteği gönder.
  const response = await fetch("/story");
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // JSON'u nesneye çevir.
  return response.json();
}

// Biten koşuyu gönderir. Sunucu seçimleri baştan oynatıp sonucu kendisi hesaplar.
export async function submitRun(story: Story, choices: Side[]): Promise<RunStats> {
  // POST isteği gönder.
  const response = await fetch("/runs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    // Java tarafı yönleri büyük harfle bekliyor. Yeni runId aynı oyunun iki kez sayılmasını önler.
    body: JSON.stringify({
      storyId: story.id,
      storyVersion: story.version,
      runId: newId(),
      playerId: playerId(),
      choices: choices.map((side) => side.toUpperCase()),
    }),
  });
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // İstatistikleri döndür.
  return (await response.json()).stats;
}
