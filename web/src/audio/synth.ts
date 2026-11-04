// Tüm sesler kodla üretiliyor: Dosya indirmek yok, lisans sorunu yok.
// Web Audio: Ses kaynaklarını (osilatör, gürültü) filtre ve ses seviyesine bağlıyoruz.

// Tercihi saklamak için depo.
import { readValue, writeValue } from "../data/storage";

// Ses motoru. Tarayıcı ilk dokunuştan önce başlatmaya izin vermez, bu yüzden boş başlıyor.
let context: AudioContext | null = null;
// Tüm seslerin geçtiği ana ses seviyesi.
let master: GainNode | null = null;
// Sessiz mi?
let muted = readValue("muted") === "1";
// Motor hazır olunca çalışacak işler (ortam sesi gibi).
let onReady: ((context: AudioContext, output: GainNode) => void) | null = null;

// Motor ve ana çıkış hazırsa ikisini döndürür.
export function engine(): { context: AudioContext; master: GainNode } | null {
  // Hazır değilse null.
  return context && master ? { context, master } : null;
}

// Motor ilk açıldığında çalışacak işi kaydeder.
export function whenReady(action: (context: AudioContext, output: GainNode) => void): void {
  // İşi sakla.
  onReady = action;
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
  // Tercihi sakla.
  writeValue("muted", value ? "1" : "0");
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
    // Bekleyen işi çalıştır.
    onReady?.(context, master);
  }
  // Tarayıcı askıya aldıysa devam ettir.
  if (context.state === "suspended") void context.resume();
}

// Kısa bir ton çalar: Frekanstan frekansa kayarak söner.
export function tone(
  from: number,
  to: number,
  duration: number,
  volume: number,
  type: OscillatorType = "sine",
  delay = 0,
): void {
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
// reverse: Yükselerek biter, geri sarma hissi verir.
export function noise(
  duration: number,
  volume: number,
  frequency: number,
  type: BiquadFilterType = "lowpass",
  reverse = false,
): void {
  // Motor yoksa çık.
  if (!context || !master) return;
  // Başlangıç anı.
  const at = context.currentTime;
  // Rastgele örneklerle dolu ses tamponu.
  const buffer = context.createBuffer(
    1,
    Math.ceil(context.sampleRate * duration),
    context.sampleRate,
  );
  const data = buffer.getChannelData(0);
  for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;
  // Tamponu çalan kaynak.
  const source = context.createBufferSource();
  source.buffer = buffer;
  // Sesin karakterini belirleyen filtre.
  const filter = context.createBiquadFilter();
  filter.type = type;
  filter.frequency.value = frequency;
  // Ses seviyesi eğrisi.
  const gain = context.createGain();
  gain.gain.setValueAtTime(reverse ? 0.0001 : volume, at);
  gain.gain.exponentialRampToValueAtTime(reverse ? volume : 0.0001, at + duration);
  // Bağla ve çal.
  source.connect(filter);
  filter.connect(gain);
  gain.connect(master);
  source.start(at);
}
