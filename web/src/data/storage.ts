// Tarayıcı deposu gizli modda hata verebilir. Oyun bundan etkilenmesin diye tüm okuma ve yazmalar burada.

// Tüm anahtarların ön eki.
const PREFIX = "till-midnight.";

// Bir değeri okur. Yoksa ya da depo çalışmıyorsa null döner.
export function readValue(key: string): string | null {
  try {
    // Değeri oku.
    return localStorage.getItem(PREFIX + key);
  } catch {
    // Depo yok.
    return null;
  }
}

// Bir değeri yazar. Yazılamazsa değer sadece bu oturumda geçerli olur.
export function writeValue(key: string, value: string): void {
  try {
    // Değeri yaz.
    localStorage.setItem(PREFIX + key, value);
  } catch {
    // Oyun deposuz da çalışır.
  }
}
