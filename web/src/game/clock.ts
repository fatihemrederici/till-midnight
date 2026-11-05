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

// Saati bölümün ilerleme oranına çevirir: 0 bölümün başlangıç saati, 1 gece yarısı.
export function dayProgress(time: string, start: string): number {
  // Başlangıçtan bu yana geçen oran.
  const progress = (toMinutes(time) - toMinutes(start)) / (MIDNIGHT - toMinutes(start));
  // 0 ile 1 arasında tut.
  return Math.max(0, Math.min(1, progress));
}
