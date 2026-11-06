// Ses üreticileri.
import { noise, tone } from "./synth";

// Oyundaki tek seferlik sesler.
export const sfx = {
  // Kart uçarken hışırtı.
  swipe: () => noise(0.18, 0.22, 2400, "bandpass"),
  // Kaydırma karar eşiğini geçince küçük tık.
  snap: () => tone(1100, 700, 0.03, 0.05, "square"),
  // Saatin ilerlemesi.
  tick: () => {
    tone(1800, 1200, 0.025, 0.06, "square");
    tone(1400, 900, 0.025, 0.05, "square", 0.14);
  },
  // Gösterge düştüğünde alçak darbe.
  thud: () => {
    tone(90, 40, 0.22, 0.4);
    noise(0.08, 0.1, 500);
  },
  // Gösterge yükseldiğinde hafif bir nefes.
  relief: () => tone(330, 440, 0.25, 0.05, "triangle"),
  // Kriz kartı: Alarm gibi inen iki ton.
  crisis: () => {
    tone(740, 370, 0.3, 0.14, "sawtooth");
    tone(740, 370, 0.3, 0.14, "sawtooth", 0.34);
    tone(70, 35, 0.6, 0.5);
  },
  // Televizyon paraziti.
  static: () => noise(0.32, 0.16, 4200, "highpass"),
  // Kehanetteki her görüntü: Darbe ve cızırtı.
  flash: () => {
    tone(70, 30, 0.5, 0.5);
    noise(0.35, 0.22, 3000, "highpass");
  },
  // Kehanetin sonu: Cam ve su.
  crash: () => {
    noise(1.2, 0.5, 1800, "highpass");
    noise(1.6, 0.5, 300);
    tone(120, 25, 1.4, 0.6, "sawtooth");
  },
  // Ölüm: Birbirine uyumsuz sesler ve alçak bir düşüş.
  death: () => {
    [233, 247, 311, 370].forEach((frequency) =>
      tone(frequency, frequency / 2.2, 1.5, 0.14, "sawtooth"),
    );
    tone(80, 22, 1.6, 0.7);
    noise(0.5, 0.35, 800);
  },
  // Zincirin her adımı.
  chain: () => tone(160, 90, 0.16, 0.25),
  // Zamanın geri sarılması.
  rewind: () => {
    noise(1.1, 0.3, 1600, "bandpass", true);
    tone(200, 1600, 1.1, 0.1, "sawtooth");
  },
  // Kurtuluş: Yumuşak, geç gelen bir akor.
  win: () =>
    [196, 293.66, 392, 493.88].forEach((frequency, i) =>
      tone(frequency, frequency, 2.4, 0.13, "triangle", i * 0.18),
    ),
};
