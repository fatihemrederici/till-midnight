// Tipleri içeri alıyoruz.
import type { Attempt, PublicScene, Touch } from "./types";

// Bugünün sahnesini sunucudan alır. async: bekleyen işlem. Swift'teki async gibi.
export async function fetchTodaysScene(): Promise<PublicScene> {
  // İsteği gönder ve cevabı bekle.
  const response = await fetch("/scene/today");
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // Gelen JSON'u nesneye çevir.
  return response.json();
}

// Dokunuşları sunucuya gönderir, olacakları alır.
export async function playAttempt(sceneId: string, touches: Touch[]): Promise<Attempt> {
  // POST isteği gönder.
  const response = await fetch("/attempt", {
    method: "POST",
    // Gövdenin JSON olduğunu söyle.
    headers: { "Content-Type": "application/json" },
    // Nesneyi JSON metnine çevir.
    body: JSON.stringify({ sceneId, touches }),
  });
  // Hata kodu geldiyse dur.
  if (!response.ok) throw new Error(`Server returned ${response.status}`);
  // Gelen JSON'u nesneye çevir.
  return response.json();
}