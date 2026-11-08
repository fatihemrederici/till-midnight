// Arka plan: Saate göre değişen müzik ve mekâna göre değişen ortam sesi.
// Dosyalar web/public/audio altında durur. Bir dosya yoksa o katman sessiz kalır, oyun bozulmaz.

// Ses motoru.
import { engine } from "./synth";
// Saatin günün hangi dilimine düştüğü.
import { dayPhase, toMinutes } from "../game/clock";
// Kartın geçtiği yer.
import type { Place } from "../game/types";

// İki katman: Müzik ve ortam sesi.
type LayerName = "music" | "ambience";
// Bir katmanda çalan parça.
type Playing = { name: string; source: AudioBufferSourceNode; gain: GainNode };

// Geçişlerin süresi (saniye): Parçalar birbirine yavaşça karışır. Müzik daha uzun sürede değişir.
const CROSSFADE: Record<LayerName, number> = { music: 5, ambience: 2.5 };
// Gece müziğinin başladığı saat (dakika).
const NIGHT_FROM = 22 * 60;
// Katmanların ses seviyesi. Efektlerin altında kalsınlar.
const VOLUME: Record<LayerName, number> = { music: 0.32, ambience: 0.22 };

// Yüklenen ses dosyaları. Aynı dosya iki kez indirilmez.
const buffers = new Map<string, Promise<AudioBuffer | null>>();
// Her katmanda şu an çalan parça.
const playing: Record<LayerName, Playing | null> = { music: null, ambience: null };
// Her katmanda çalması istenen parça. Yükleme bitince hâlâ isteniyor mu diye bakılır.
const wanted: Record<LayerName, string | null> = { music: null, ambience: null };

// Müzik: Eve gelene kadar gündüz, evde 22:00'ye kadar akşam, sonra gece.
export function musicFor(time: string, place: Place): string {
  if (toMinutes(time) >= NIGHT_FROM) return "night";
  return place === "home" ? "evening" : "day";
}

// Yere ve saate göre ortam sesi: Akşam fırtına başlar, evde yağmur duyulur.
export function ambienceFor(place: Place, time: string): string {
  const phase = dayPhase(time);
  if (place === "home" && (phase === "night" || phase === "late")) return "home-rain";
  return place;
}

// Ses dosyasını indirir ve çözer. Dosya yoksa ya da bozuksa null.
function load(path: string): Promise<AudioBuffer | null> {
  const audio = engine();
  if (!audio) return Promise.resolve(null);
  let buffer = buffers.get(path);
  if (!buffer) {
    buffer = fetch(path)
      .then((response) => (response.ok ? response.arrayBuffer() : null))
      .then((data) => (data ? audio.context.decodeAudioData(data) : null))
      .catch(() => null);
    buffers.set(path, buffer);
  }
  return buffer;
}

// Çalan parçayı verilen sürede kısıp durdurur.
function fadeOut(layer: LayerName, seconds: number): void {
  const audio = engine();
  const current = playing[layer];
  if (!audio || !current) return;
  const now = audio.context.currentTime;
  current.gain.gain.cancelScheduledValues(now);
  current.gain.gain.setValueAtTime(current.gain.gain.value, now);
  current.gain.gain.linearRampToValueAtTime(0, now + seconds);
  current.source.stop(now + seconds + 0.05);
  playing[layer] = null;
}

// Katmanda istenen parçayı çalar. Aynı parça zaten çalıyorsa dokunmaz.
async function play(layer: LayerName, name: string): Promise<void> {
  wanted[layer] = name;
  if (playing[layer]?.name === name) return;
  const buffer = await load(`/audio/${layer}/${name}.mp3`);
  const audio = engine();
  // Yüklenirken oyuncu başka yere geçtiyse ya da dosya yoksa vazgeç.
  if (!audio || wanted[layer] !== name || playing[layer]?.name === name) return;
  fadeOut(layer, CROSSFADE[layer]);
  if (!buffer) return;
  // Yeni parça sessiz başlar ve yavaşça yükselir. Döngüde çalar.
  const source = audio.context.createBufferSource();
  source.buffer = buffer;
  source.loop = true;
  const gain = audio.context.createGain();
  const now = audio.context.currentTime;
  gain.gain.setValueAtTime(0, now);
  gain.gain.linearRampToValueAtTime(VOLUME[layer], now + CROSSFADE[layer]);
  source.connect(gain);
  gain.connect(audio.master);
  // Parçanın hep başından değil, rastgele bir yerinden başla: Her oyunda aynı an duyulmasın.
  source.start(now, Math.random() * buffer.duration);
  playing[layer] = { name, source, gain };
}

// Kart değişince çağrılır: Saate ve yere uyan müziği ve ortam sesini çalar.
export function setScore(time: string, place: Place): void {
  void play("music", musicFor(time, place));
  void play("ambience", ambienceFor(place, time));
}

// Oyundan çıkınca çağrılır. Ölümde müzik bir anda kesilir, bu yüzden geçiş kısa.
export function stopScore(seconds = 0.15): void {
  wanted.music = null;
  wanted.ambience = null;
  fadeOut("music", seconds);
  fadeOut("ambience", seconds);
}
