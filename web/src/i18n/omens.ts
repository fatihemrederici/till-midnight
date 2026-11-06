// Ölüm cümleleri: Akıl düşükken kart metninin yerine bir anlığına geçer.
// Her biri bölümün bir ölümünü ima eder ama adını koymaz.
import type { Omen } from "../game/omens";
// Tipler.
import type { Language, Text } from "../game/types";
// Bölümü olmayan cümleler.
import { HALLUCINATIONS } from "./whispers";

// Bölüm kimliği -> ölüm kimliği -> cümle.
export const OMENS: Record<string, Record<string, Text>> = {
  "first-day": {
    road: { en: "A horn. Very close.", tr: "Bir korna. Çok yakın.", pl: "Klakson. Bardzo blisko." },
    stairs: { en: "The stairs are wet.", tr: "Basamaklar ıslak.", pl: "Schody są mokre." },
    balcony: { en: "It’s a long way down.", tr: "Aşağısı çok uzak.", pl: "Daleko w dół." },
    kitchen: { en: "The oil is hissing.", tr: "Yağ cızırdıyor.", pl: "Olej skwierczy." },
    fuse: { en: "The wire is bare.", tr: "Kablo çıplak.", pl: "Kabel jest goły." },
    railing: { en: "The railing creaks.", tr: "Korkuluk gıcırdıyor.", pl: "Barierka skrzypi." },
    wardrobe: {
      en: "The wardrobe is leaning.",
      tr: "Gardırop öne eğiliyor.",
      pl: "Szafa się przechyla.",
    },
    panic: { en: "A bell. A bicycle.", tr: "Bir zil. Bir bisiklet.", pl: "Dzwonek. Rower." },
    collapse: { en: "Your head is spinning.", tr: "Başın dönüyor.", pl: "Kręci ci się w głowie." },
    alone: {
      en: "Your phone never rings.",
      tr: "Telefonun hiç çalmıyor.",
      pl: "Telefon nie dzwoni.",
    },
  },
  "night-shift": {
    slip: { en: "The marble is wet.", tr: "Mermer ıslak.", pl: "Marmur jest mokry." },
    shaft: {
      en: "The lift is here. Is it?",
      tr: "Asansör geldi. Geldi mi?",
      pl: "Winda przyjechała. Na pewno?",
    },
    spark: { en: "A sweet smell. Gas.", tr: "Tatlı bir koku. Gaz.", pl: "Słodki zapach. Gaz." },
    frame: { en: "The wall is cracking.", tr: "Duvar çatırdıyor.", pl: "Ściana trzeszczy." },
    iron: { en: "The bolts are loose.", tr: "Cıvatalar gevşek.", pl: "Śruby są luźne." },
    vision: { en: "Third floor.", tr: "Üçüncü kat.", pl: "Trzecie piętro." },
    panic: { en: "Where is the exit?", tr: "Çıkış nerede?", pl: "Gdzie jest wyjście?" },
    collapse: {
      en: "Lie down. Just a minute.",
      tr: "Uzan. Bir dakika.",
      pl: "Połóż się. Na minutę.",
    },
    alone: { en: "Nobody is coming.", tr: "Kimse gelmeyecek.", pl: "Nikt nie przyjdzie." },
  },
  "last-dance": {
    "sam-throat": { en: "Sam is coughing.", tr: "Sam öksürüyor.", pl: "Sam kaszle." },
    panel: {
      en: "Something hums in the wall.",
      tr: "Duvarın içinde bir şey vızıldıyor.",
      pl: "Coś brzęczy w ścianie.",
    },
    chandelier: {
      en: "The chandelier is swaying.",
      tr: "Avize sallanıyor.",
      pl: "Żyrandol się kołysze.",
    },
    trapped: { en: "The door is locked.", tr: "Kapı kilitli.", pl: "Drzwi są zamknięte." },
    chest: { en: "Your chest is tightening.", tr: "Göğsün sıkışıyor.", pl: "Ściska cię w piersi." },
    "best-man": { en: "Your knees are shaking.", tr: "Dizlerin titriyor.", pl: "Kolana ci drżą." },
    uninvited: {
      en: "Nobody believes you.",
      tr: "Kimse sana inanmıyor.",
      pl: "Nikt ci nie wierzy.",
    },
  },
};

// Bölümde cümlesi yazılmış ölümler.
export function knownOmens(storyId: string): string[] {
  return Object.keys(OMENS[storyId] ?? {});
}

// Cümlenin seçilen dildeki metni.
export function omenText(storyId: string, omen: Omen, language: Language): string {
  // Bölümün kendi cümlesi.
  if ("ending" in omen) {
    const line = OMENS[storyId]?.[omen.ending];
    if (line) return line[language];
  }
  // Genel cümle.
  const lines = HALLUCINATIONS[language];
  return lines[("generic" in omen ? omen.generic : 0) % lines.length];
}
