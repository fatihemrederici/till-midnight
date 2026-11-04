// Tipler.
import type { Language, Text } from "./types";

// Desteklenen diller ve seçicide görünen adları.
export const LANGUAGES: { id: Language; label: string }[] = [
  { id: "en", label: "EN" },
  { id: "tr", label: "TR" },
  { id: "pl", label: "PL" },
];

// Dilin tarayıcıda saklandığı anahtar.
const STORE_KEY = "till-midnight.language";

// Açılışta kullanılacak dil: Önce seçilmiş dil, sonra telefonun dili, yoksa İngilizce.
export function initialLanguage(): Language {
  try {
    // Saklı dil.
    const saved = localStorage.getItem(STORE_KEY);
    if (saved === "en" || saved === "tr" || saved === "pl") return saved;
  } catch {
    // Gizli modda depo çalışmayabilir; aşağıdaki yöntemle devam ederiz.
  }
  // Telefonun dilinin ilk iki harfi.
  const device = (navigator.language || "en").slice(0, 2);
  // Destekleniyorsa onu seç.
  return device === "tr" || device === "pl" ? device : "en";
}

// Seçilen dili saklar.
export function saveLanguage(language: Language): void {
  try {
    // Sakla.
    localStorage.setItem(STORE_KEY, language);
  } catch {
    // Saklanamazsa dil sadece bu oturumda geçerli olur.
  }
}

// Hikâye dışındaki tüm arayüz yazıları.
export const UI = {
  tagline: {
    en: "You should have died this morning. Swipe to get through the day. One small choice can kill you hours later.",
    tr: "Bu sabah ölmen gerekiyordu. Günü atlatmak için kartları kaydır. Küçük bir karar saatler sonra seni öldürebilir.",
    pl: "Dziś rano miałeś zginąć. Przesuwaj karty, żeby przetrwać dzień. Jeden drobny wybór może zabić cię kilka godzin później.",
  },
  start: { en: "See what happens", tr: "Neler olacağını gör", pl: "Zobacz, co się stanie" },
  continue: { en: "Open your eyes", tr: "Gözünü aç", pl: "Otwórz oczy" },
  skip: { en: "Skip", tr: "Geç", pl: "Pomiń" },
  deaths: { en: "Your deaths", tr: "Ölümlerin", pl: "Twoje śmierci" },
  found: { en: "Found", tr: "Bulunan", pl: "Znalezione" },
  back: { en: "Back", tr: "Geri", pl: "Wstecz" },
  goal: { en: "Survive until 00:00", tr: "00:00'a kadar yaşa", pl: "Przetrwaj do 00:00" },
  attempt: { en: "Try", tr: "Deneme", pl: "Próba" },
  chain: { en: "How death found you", tr: "Ölüm seni böyle buldu", pl: "Tak znalazła cię śmierć" },
  rewind: { en: "Rewind time", tr: "Zamanı geri sar", pl: "Cofnij czas" },
  again: { en: "Play again", tr: "Yeniden oyna", pl: "Zagraj ponownie" },
  soundOn: { en: "Sound on", tr: "Ses açık", pl: "Dźwięk włączony" },
  soundOff: { en: "Sound off", tr: "Ses kapalı", pl: "Dźwięk wyłączony" },
  language: { en: "Language", tr: "Dil", pl: "Język" },
  loading: { en: "Loading", tr: "Yükleniyor", pl: "Ładowanie" },
  error: {
    en: "Can't reach the game server. Start the database with docker compose up -d and the server with ./mvnw spring-boot:run, then reload this page.",
    tr: "Oyun sunucusuna ulaşılamıyor. Veritabanını docker compose up -d, sunucuyu ./mvnw spring-boot:run ile başlatıp bu sayfayı yenile.",
    pl: "Brak połączenia z serwerem gry. Uruchom bazę poleceniem docker compose up -d, a serwer poleceniem ./mvnw spring-boot:run, potem odśwież stronę.",
  },
  shareOfPlayers: {
    en: "{percent}% of runs end this way.",
    tr: "Oyunların %{percent}'i böyle bitiyor.",
    pl: "{percent}% gier kończy się w ten sposób.",
  },
} satisfies Record<string, Text>;
