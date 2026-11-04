// Ses motoru ve ton üretici.
import { engine, tone, whenReady } from "./synth";

// Ortam sesinin ses seviyesi.
let droneGain: GainNode | null = null;
// Kalp atışı zamanlayıcısı.
let heartTimer: number | null = null;

// Ses motoru açılınca uğultuyu başlat.
whenReady((context, output) => {
  // Ortam sesi seviyesi, sessiz başlar.
  droneGain = context.createGain();
  droneGain.gain.value = 0;
  // Keskin sesleri kesen filtre.
  const filter = context.createBiquadFilter();
  filter.type = "lowpass";
  filter.frequency.value = 170;
  filter.connect(droneGain);
  droneGain.connect(output);
  // Birbirine yakın iki frekans yavaş bir dalgalanma yaratır.
  [55, 55.7].forEach((frequency) => {
    const oscillator = context.createOscillator();
    oscillator.type = "sawtooth";
    oscillator.frequency.value = frequency;
    oscillator.connect(filter);
    oscillator.start();
  });
});

// Ortam sesinin seviyesini yumuşakça değiştirir.
export function setDrone(level: number): void {
  // Motor.
  const audio = engine();
  // Hazır değilse çık.
  if (!audio || !droneGain) return;
  // Bir saniyede yeni seviyeye geç.
  droneGain.gain.setTargetAtTime(level, audio.context.currentTime, 0.6);
}

// Kalp atışını ayarlar: 0 durdurur, sayı ise atışlar arası milisaniyedir.
export function setHeartbeat(interval: number): void {
  // Eskisini durdur.
  if (heartTimer !== null) window.clearInterval(heartTimer);
  heartTimer = null;
  // 0 ise atış yok.
  if (interval <= 0) return;
  // Çift vuruş: Küt-küt.
  const beat = () => {
    tone(60, 38, 0.13, 0.55);
    tone(52, 34, 0.15, 0.4, "sine", 0.18);
  };
  // Hemen başla, sonra tekrarla.
  beat();
  heartTimer = window.setInterval(beat, interval);
}
