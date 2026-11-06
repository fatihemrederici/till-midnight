// Oyunun saat hesapları.

// Gece yarısının dakikası. Her bölüm burada biter.
const MIDNIGHT = 24 * 60;

// "14:30" saatini dakikaya çevirir.
export function toMinutes(time: string): number {
  // Saat ve dakikayı ayır.
  const [hours, minutes] = time.split(":").map(Number);
  // Toplam dakika.
  return hours * 60 + minutes;
}

// Dakikayı "08:14" biçimine çevirir.
export function formatMinutes(total: number): string {
  // Saat ve dakika, gerekiyorsa başına sıfır eklenir.
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}

// Günün ışığı: Arka plan saate göre değişir.
export type Phase = "morning" | "noon" | "afternoon" | "night" | "late";

// Saatin hangi ışığa düştüğü.
export function dayPhase(time: string): Phase {
  const minutes = toMinutes(time);
  // Sabah: 10:00'a kadar.
  if (minutes < 10 * 60) return "morning";
  // Öğle: 15:00'e kadar.
  if (minutes < 15 * 60) return "noon";
  // Akşamüstü: 19:00'a kadar.
  if (minutes < 19 * 60) return "afternoon";
  // Gece: 23:00'e kadar, sonrası son saat.
  return minutes < 23 * 60 ? "night" : "late";
}

// Saati bölümün ilerleme oranına çevirir: 0 bölümün başlangıç saati, 1 gece yarısı.
export function dayProgress(time: string, start: string): number {
  // Başlangıçtan bu yana geçen oran.
  const progress = (toMinutes(time) - toMinutes(start)) / (MIDNIGHT - toMinutes(start));
  // 0 ile 1 arasında tut.
  return Math.max(0, Math.min(1, progress));
}
