// Ses üreticileri.
import { engine, noise } from "./synth";

// Etkinin gücü: 0 yok, 1 hafif, 2 güçlü.
export type Level = 0 | 1 | 2;

// Kulak çınlamasının kaynağı ve ses seviyesi.
let ringing: { oscillator: OscillatorNode; gain: GainNode } | null = null;
// Nefes ve fısıltı zamanlayıcıları.
let breathTimer: number | null = null;
let whisperTimer: number | null = null;

// Akıl: Kulak çınlaması. İnce, kesilmeyen bir ton.
export function setRinging(level: Level): void {
  // Motor hazır değilse çık.
  const audio = engine();
  if (!audio) return;
  // İlk kez kullanılıyorsa kaynağı oluştur.
  if (!ringing) {
    const oscillator = audio.context.createOscillator();
    oscillator.frequency.value = 5600;
    const gain = audio.context.createGain();
    gain.gain.value = 0;
    oscillator.connect(gain);
    gain.connect(audio.master);
    oscillator.start();
    ringing = { oscillator, gain };
  }
  // Sesi yumuşakça ayarla.
  ringing.gain.gain.setTargetAtTime([0, 0.004, 0.012][level], audio.context.currentTime, 0.4);
}

// Bir zamanlayıcıyı değiştirir. Aralık 0 ise durdurur.
function retime(current: number | null, interval: number, action: () => void): number | null {
  // Eskisini durdur.
  if (current !== null) window.clearInterval(current);
  // Yeni aralık yoksa bitir.
  if (interval <= 0) return null;
  // Hemen çal ve tekrarla.
  action();
  return window.setInterval(action, interval);
}

// Sağlık: Ağır nefes. İçeri ve dışarı iki yumuşak hışırtı.
export function setBreathing(level: Level): void {
  breathTimer = retime(breathTimer, [0, 4200, 2800][level], () => {
    noise(1.1, 0.05, 700, "bandpass", true);
    window.setTimeout(() => noise(1.3, 0.06, 450, "bandpass"), 1200);
  });
}

// Güven: Fısıltı. Tiz, kısa, anlaşılmayan hışırtılar.
export function setWhispering(level: Level): void {
  whisperTimer = retime(whisperTimer, [0, 3600, 2200][level], () => {
    noise(0.22, 0.035, 3400, "bandpass");
    window.setTimeout(() => noise(0.3, 0.03, 2600, "bandpass"), 320);
  });
}

// Tüm sesleri susturur. Ekrandan çıkınca çağrılır.
export function stopMoods(): void {
  setRinging(0);
  setBreathing(0);
  setWhispering(0);
}
