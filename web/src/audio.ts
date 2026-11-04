// Tüm sesler kodla üretiliyor: dosya indirmek yok, lisans sorunu yok.
// Web Audio: ses kaynaklarını (osilatör, gürültü) filtre ve ses seviyesine bağlıyoruz.

// Ses tercihinin saklandığı anahtar.
const MUTE_KEY = "till-midnight.muted";

// Ses motoru. Tarayıcı ilk dokunuştan önce başlatmaya izin vermez, bu yüzden boş başlıyor.
let context: AudioContext | null = null;
// Tüm seslerin geçtiği ana ses seviyesi.
let master: GainNode | null = null;
// Ortam sesinin ses seviyesi.
let droneGain: GainNode | null = null;
// Kalp atışı zamanlayıcısı.
let heartTimer: number | null = null;
// Sessiz mi?
let muted = readMuted();

// Saklı tercihi okur.
function readMuted(): boolean {
  try {
    // "1" sessiz demektir.
    return localStorage.getItem(MUTE_KEY) === "1";
  } catch {
    // Depo yoksa ses açık.
    return false;
  }
}

// Sessiz mi?
export function isMuted(): boolean {
  // Değeri döndür.
  return muted;
}

// Sesi açar ya da kapatır.
export function setMuted(value: boolean): void {
  // Değeri kaydet.
  muted = value;
  try {
    // Tercihi sakla.
    localStorage.setItem(MUTE_KEY, value ? "1" : "0");
  } catch {
    // Saklanamazsa sadece bu oturumda geçerli.
  }
  // Çalan sesi hemen etkile.
  if (master && context) master.gain.setTargetAtTime(value ? 0 : 1, context.currentTime, 0.02);
}

// İlk dokunuşta çağrılır: Ses motorunu başlatır.
export function unlockAudio(): void {
  // Ses desteği yoksa oyun sessiz çalışır.
  const Engine = window.AudioContext;
  if (!Engine) return;
  // İlk kez oluştur.
  if (!context) {
    context = new Engine();
    // Ana ses seviyesi.
    master = context.createGain();
    master.gain.value = muted ? 0 : 1;
    master.connect(context.destination);
    // Uğultuyu başlat.
    startDrone();
  }
  // Tarayıcı askıya aldıysa devam ettir.
  if (context.state === "suspended") void context.resume();
}

// Sürekli çalan alçak uğultu: Sessizliği gergin yapan şey bu.
function startDrone(): void {
  // Motor yoksa çık.
  if (!context || !master) return;
  // Ortam sesi seviyesi.
  droneGain = context.createGain();
  droneGain.gain.value = 0.0;
  // Keskin sesleri kesen filtre.
  const filter = context.createBiquadFilter();
  filter.type = "lowpass";
  filter.frequency.value = 170;
  filter.connect(droneGain);
  droneGain.connect(master);
  // Birbirine yakın iki frekans yavaş bir dalgalanma yaratır.
  [55, 55.7].forEach((frequency) => {
    const oscillator = context!.createOscillator();
    oscillator.type = "sawtooth";
    oscillator.frequency.value = frequency;
    oscillator.connect(filter);
    oscillator.start();
  });
}

// Ortam sesinin seviyesini yumuşakça değiştirir.
export function setDrone(level: number): void {
  // Motor yoksa çık.
  if (!context || !droneGain) return;
  // Bir saniyede yeni seviyeye geç.
  droneGain.gain.setTargetAtTime(level, context.currentTime, 0.6);
}

// Kısa bir ton çalar: frekanstan frekansa kayarak söner.
function tone(from: number, to: number, duration: number, volume: number, type: OscillatorType = "sine", delay = 0): void {
  // Motor yoksa çık.
  if (!context || !master) return;
  // Başlangıç anı.
  const at = context.currentTime + delay;
  // Ton kaynağı.
  const oscillator = context.createOscillator();
  oscillator.type = type;
  oscillator.frequency.setValueAtTime(from, at);
  oscillator.frequency.exponentialRampToValueAtTime(Math.max(1, to), at + duration);
  // Ses seviyesi eğrisi.
  const gain = context.createGain();
  gain.gain.setValueAtTime(volume, at);
  gain.gain.exponentialRampToValueAtTime(0.0001, at + duration);
  // Bağla ve çal.
  oscillator.connect(gain);
  gain.connect(master);
  oscillator.start(at);
  oscillator.stop(at + duration + 0.02);
}

// Kısa bir gürültü patlaması çalar: Kaydırma, darbe, cam sesi hep bundan.
function noise(duration: number, volume: number, frequency: number, type: BiquadFilterType = "lowpass", delay = 0, reverse = false): void {
  // Motor yoksa çık.
  if (!context || !master) return;
  // Başlangıç anı.
  const at = context.currentTime + delay;
  // Rastgele örneklerle dolu ses tamponu.
  const buffer = context.createBuffer(1, Math.ceil(context.sampleRate * duration), context.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;
  // Tamponu çalan kaynak.
  const source = context.createBufferSource();
  source.buffer = buffer;
  // Sesin karakterini belirleyen filtre.
  const filter = context.createBiquadFilter();
  filter.type = type;
  filter.frequency.value = frequency;
  // Ses seviyesi eğrisi. reverse: yükselerek biter, geri sarma hissi verir.
  const gain = context.createGain();
  gain.gain.setValueAtTime(reverse ? 0.0001 : volume, at);
  gain.gain.exponentialRampToValueAtTime(reverse ? volume : 0.0001, at + duration);
  // Bağla ve çal.
  source.connect(filter);
  filter.connect(gain);
  gain.connect(master);
  source.start(at);
}

// Oyundaki sesler.
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
    [233, 247, 311, 370].forEach((frequency) => tone(frequency, frequency / 2.2, 1.5, 0.14, "sawtooth"));
    tone(80, 22, 1.6, 0.7);
    noise(0.5, 0.35, 800);
  },
  // Zincirin her adımı.
  chain: () => tone(160, 90, 0.16, 0.25),
  // Zamanın geri sarılması.
  rewind: () => {
    noise(1.1, 0.3, 1600, "bandpass", 0, true);
    tone(200, 1600, 1.1, 0.1, "sawtooth");
  },
  // Kurtuluş: Yumuşak, geç gelen bir akor.
  win: () => [196, 293.66, 392, 493.88].forEach((frequency, i) => tone(frequency, frequency, 2.4, 0.13, "triangle", i * 0.18)),
};

// Kalp atışını ayarlar: 0 durdurur, sayı ise atışlar arası milisaniyedir.
export function setHeartbeat(interval: number): void {
  // Eskisini durdur.
  if (heartTimer !== null) window.clearInterval(heartTimer);
  heartTimer = null;
  // 0 ise atış yok.
  if (interval <= 0) return;
  // Çift vuruş: küt-küt.
  const beat = () => {
    tone(60, 38, 0.13, 0.55);
    tone(52, 34, 0.15, 0.4, "sine", 0.18);
  };
  // Hemen başla, sonra tekrarla.
  beat();
  heartTimer = window.setInterval(beat, interval);
}

// Telefonu titretir. Desteklemeyen cihazlarda hiçbir şey yapmaz (iPhone Safari gibi).
export function vibrate(pattern: number | number[]): void {
  try {
    // Destek varsa titret.
    navigator.vibrate?.(pattern);
  } catch {
    // Titreşim olmadan da oyun çalışır.
  }
}
