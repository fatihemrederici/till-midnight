// Ses üreticileri.
import { engine } from "./synth";

// Etkinin gücü: 0 yok, 1 hafif, 2 güçlü.
export type Level = 0 | 1 | 2;

// Kulak çınlamasının kaynağı ve ses seviyesi.
let ringing: { oscillator: OscillatorNode; gain: GainNode } | null = null;

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

// Tüm sesleri susturur. Ekrandan çıkınca çağrılır.
export function stopMoods(): void {
  setRinging(0);
}
