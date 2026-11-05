// Tip.
import type { Text } from "../game/types";

// Hikâye dışındaki tüm arayüz yazıları.
export const UI = {
  gameTitle: { en: "Till Midnight", tr: "Gece Yarısına Kadar", pl: "Do północy" },
  tagline: {
    en: "You saw how you die. Swipe to reach midnight. One small choice can kill you hours later.",
    tr: "Nasıl öleceğini gördün. Gece yarısına ulaşmak için kartları kaydır. Küçük bir karar saatler sonra seni öldürebilir.",
    pl: "Widzisz, jak umierasz. Przesuwaj karty, żeby dotrwać do północy. Jeden drobny wybór może zabić kilka godzin później.",
  },
  chapters: { en: "Chapters", tr: "Bölümler", pl: "Rozdziały" },
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
