// Bu tipler Java'daki record'ların TypeScript karşılığı. Swift'teki struct tanımları gibi.

// Nesnenin ızgaradaki konumu.
export type Position = { x: number; y: number };

// GET /scene/today cevabındaki tek bir nesne.
export type PublicObject = {
  id: string;
  label: string;
  position: Position;
  emoji: string;
};

// GET /scene/today cevabı.
export type PublicScene = {
  id: string;
  title: string;
  intro: string;
  durationSeconds: number;
  maxTouches: number;
  grid: { columns: number; rows: number };
  objects: PublicObject[];
};

// Oyuncunun bir dokunuşu.
export type Touch = { object: string; second: number };

// Sunucunun gönderdiği tek bir olay.
export type GameEvent = {
  second: number;
  object: string;
  emoji: string;
  // Sunucu açıklama göndermeyebilir, bu yüzden null olabilir. Swift'teki String? gibi.
  caption: string | null;
  noEffect: boolean;
};

// POST /attempt cevabı.
export type Attempt = {
  won: boolean;
  endSecond: number;
  disasterCaption: string | null;
  events: GameEvent[];
};