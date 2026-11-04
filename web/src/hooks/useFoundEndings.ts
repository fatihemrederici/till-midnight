// React araçları.
import { useCallback, useState } from "react";
// Depo.
import { readValue, writeValue } from "../data/storage";

// Saklı sonları okur.
function readFound(): string[] {
  try {
    // Yoksa boş liste.
    return JSON.parse(readValue("found") ?? "[]");
  } catch {
    // Kayıt bozuksa sıfırdan başla.
    return [];
  }
}

// Oyuncunun bulduğu sonlar. Sayfa kapansa da hatırlanır.
export function useFoundEndings() {
  // Bulunan sonlar.
  const [found, setFound] = useState<string[]>(readFound);

  // Yeni bulunan sonu ekler.
  const addFound = useCallback((endingId: string) => {
    setFound((current) => {
      // Zaten varsa değişiklik yok.
      if (current.includes(endingId)) return current;
      // Listeye ekle ve sakla.
      const updated = [...current, endingId];
      writeValue("found", JSON.stringify(updated));
      return updated;
    });
  }, []);

  // Listeyi ve ekleme fonksiyonunu döndür.
  return { found, addFound };
}
