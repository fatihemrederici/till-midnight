// Oyunun saat hesapları.

// Oyunun başladığı ve bittiği saatler.
const DAY_START_HOUR = 8;
const DAY_END_HOUR = 24;

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

// Saati günün ilerleme oranına çevirir: 0 başlangıç, 1 gece yarısı.
export function dayProgress(time: string): number {
  // Başlangıçtan bu yana geçen oran.
  const progress = (toMinutes(time) / 60 - DAY_START_HOUR) / (DAY_END_HOUR - DAY_START_HOUR);
  // 0 ile 1 arasında tut.
  return Math.max(0, Math.min(1, progress));
}
