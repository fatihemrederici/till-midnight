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
  // Oyuncunun başta göreceği kehanet.
  vision: Vision;
};

// Kehanet: oyuncu bir şey yapmazsa olacak her şey.
export type Vision = {
  events: GameEvent[];
  disasters: { second: number; caption: string }[];
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
  // En az kaç dokunuşla çözülebilir. Sadece kazanınca gelir.
  bestTouches: number | null;
  events: GameEvent[];
};

// GET /levels cevabındaki bir bölüm.
export type Level = {
  number: number;
  sceneId: string;
  title: string;
  unlocked: boolean;
  completed: boolean;
  star: boolean;
};

// GET /stats cevabı.
export type SceneStats = {
  attempts: number;
  players: number;
  solvers: number;
  medianTriesToSolve: number | null;
  losses: { second: number; count: number }[];
};