// React araçları.
import { useCallback, useEffect, useState } from "react";
// Depo.
import { readValue, writeValue } from "../data/storage";

// Bir bölümün saklı sonlarını okur.
export function readFound(storyId: string): string[] {
  try {
    // Eski sürümde ilk bölümün kaydı "found" anahtarındaydı, ilerleme kaybolmasın.
    const legacy = storyId === "first-day" ? readValue("found") : null;
    // Yoksa boş liste.
    return JSON.parse(readValue(`found.${storyId}`) ?? legacy ?? "[]");
  } catch {
    // Kayıt bozuksa sıfırdan başla.
    return [];
  }
}

// Oyuncunun bir bölümde bulduğu sonlar. Sayfa kapansa da hatırlanır.
export function useFoundEndings(storyId: string) {
  // Bulunan sonlar.
  const [found, setFound] = useState<string[]>(() => readFound(storyId));

  // Bölüm değişince o bölümün kaydını oku.
  useEffect(() => setFound(readFound(storyId)), [storyId]);

  // Yeni bulunan sonu ekler.
  const addFound = useCallback(
    (endingId: string) => {
      setFound((current) => {
        // Zaten varsa değişiklik yok.
        if (current.includes(endingId)) return current;
        // Listeye ekle ve sakla.
        const updated = [...current, endingId];
        writeValue(`found.${storyId}`, JSON.stringify(updated));
        return updated;
      });
    },
    [storyId],
  );

  // Listeyi ve ekleme fonksiyonunu döndür.
  return { found, addFound };
}
