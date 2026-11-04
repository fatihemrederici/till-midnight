// Oyun mantığı.
import { useGame } from "./useGame";

// Ana bileşen: oyunun tüm ekranı.
export function App() {
  // Oyun durumunu ve işlemleri al.
  const game = useGame();
  // Kısa isimlerle kullanmak için.
  const { scene, phase, second, touches, attempt, bubble } = game;

  // Sunucuya ulaşılamadıysa yönlendirme göster.
  if (phase === "error") {
    return (
      <main className="page" data-mood="lost">
        <h1 className="title">Can't reach the game server</h1>
        <p className="notice">
          Start the database with <code>docker compose up -d</code> and the server with{" "}
          <code>./mvnw spring-boot:run</code>, then reload this page.
        </p>
      </main>
    );
  }

  // Sahne daha gelmediyse bekleme yazısı göster.
  if (!scene) {
    return (
      <main className="page" data-mood="ready">
        <p className="notice">Loading today's scene</p>
      </main>
    );
  }

  // Kalan dokunuş hakkı.
  const touchesLeft = scene.maxTouches - touches.length;
  // Şu an dokunulabilir mi?
  const canTouch = phase === "playing" && touchesLeft > 0;
  // Saniye çizgisindeki kutular: 0'dan 9'a.
  const seconds = Array.from({ length: scene.durationSeconds }, (_, index) => index);
  // Baloncuğun bağlı olduğu nesne.
  const anchor = scene.objects.find((object) => object.id === bubble?.object);

  // Ekranı çiz. data-mood sayfanın renklerini belirler.
  return (
    <main className="page" data-mood={phase}>
      <header className="top">
        <div>
          <p className="brand">10sec</p>
          <h1 className="title">{scene.title}</h1>
        </div>
        {/* Büyük saniye sayacı. */}
        <p className="clock">{second}</p>
      </header>

      {/* Sahne. Baloncuk nesnelerin üstünde durduğu için konumu sahneye göre hesaplanır. */}
      <section className="stage">
        <div
          className="board"
          style={{
            gridTemplateColumns: `repeat(${scene.grid.columns}, 1fr)`,
            gridTemplateRows: `repeat(${scene.grid.rows}, 1fr)`,
          }}
        >
          {/* Her nesne için bir düğme çiz. */}
          {scene.objects.map((object) => (
            <button
              key={object.id}
              className="object"
              // Izgaradaki yeri. CSS'te sayım 1'den başlar.
              style={{ gridColumn: object.position.x + 1, gridRow: object.position.y + 1 }}
              disabled={!canTouch}
              // Az önce değişen nesne vurgulanır.
              data-active={bubble?.object === object.id}
              // Etkisiz dokunulan nesne sallanır.
              data-nothing={game.nothingOn === object.id}
              onClick={() => game.touchObject(object.id)}
              aria-label={`Touch ${object.label}`}
            >
              {/* key değişince animasyon yeniden çalışır: durum değiştiğini gösterir. */}
              <span className="emoji" key={game.emojis[object.id]}>
                {game.emojis[object.id]}
              </span>
              <span className="label">{object.label}</span>
            </button>
          ))}
        </div>

        {/* Olanları anlatan baloncuk. pointer-events: none olduğu için altındaki nesneye dokunulabilir. */}
        {bubble && anchor && (
          <p
            className="bubble"
            data-danger={bubble.danger}
            aria-live="polite"
            style={bubbleStyle(anchor.position, scene.grid)}
          >
            {bubble.text}
          </p>
        )}

        {/* Uyanış yazısı sahnenin ortasında çıkar. */}
        {phase === "awake" && <p className="wake">It hasn't happened yet.</p>}
      </section>

      {/* 10 saniyelik zaman çizgisi. */}
      <ol className="timeline" aria-label="Seconds">
        {seconds.map((value) => (
          <li
            key={value}
            className="tick"
            // Geçmiş saniyeler dolu görünür.
            data-past={(phase === "vision" || phase === "playing" || phase === "lost") && value < second}
            // Şu anki saniye vurgulanır.
            data-now={(phase === "vision" || phase === "playing") && value === second}
            // Dokunuş yapılan saniye işaretlenir.
            data-touched={touches.some((touch) => touch.second === value)}
          >
            {value}
          </li>
        ))}
      </ol>

      {/* Alt kısım: yönlendirme ve düğmeler. */}
      <section className="panel">
        {phase === "ready" && (
          <>
            <p className="hint">{scene.intro}</p>
            <p className="hint">You will see how this ends. Then you get ten seconds to stop it.</p>
            <button className="action" onClick={game.startVision}>
              See what happens
            </button>
          </>
        )}

        {phase === "vision" && (
          <>
            <p className="hint strong">You see how it ends.</p>
            <button className="action quiet" onClick={game.skipVision}>
              Skip
            </button>
          </>
        )}

        {phase === "playing" && (
          <p className="hint strong">
            {touchesLeft > 0
              ? `Stop it. ${touchesLeft} ${touchesLeft === 1 ? "touch" : "touches"} left.`
              : "No touches left. Watch what happens."}
          </p>
        )}

        {phase === "lost" && (
          <div className="actions">
            <button className="action" onClick={game.startRun}>
              Rewind
            </button>
            <button className="action quiet" onClick={game.startVision}>
              See the vision again
            </button>
          </div>
        )}

        {phase === "won" && (
          <>
            <p className="caption">You stopped it. Nobody will ever know.</p>
            <p className="hint">
              Done on try {game.attemptCount} with {touches.length} {touches.length === 1 ? "touch" : "touches"}.{" "}
              {attempt?.bestTouches === touches.length
                ? "That is the fewest possible."
                : `It can be done with ${attempt?.bestTouches}.`}
            </p>
            {game.stats && (
              <p className="hint">
                {game.stats.solvers} of {game.stats.players} {game.stats.players === 1 ? "player" : "players"} stopped
                it today.
              </p>
            )}
            <button className="action" onClick={game.startRun}>
              Play again
            </button>
          </>
        )}
      </section>
    </main>
  );
}

// Baloncuğun konumunu hesaplar: nesnenin hemen üstünde, üst satırdaysa altında.
function bubbleStyle(position: { x: number; y: number }, grid: { columns: number; rows: number }) {
  // Nesnenin ortasının sahne genişliğindeki yüzdesi. Kenarlardan taşmasın diye sınırlıyoruz.
  const center = Math.min(64, Math.max(36, ((position.x + 0.5) / grid.columns) * 100));
  // En üst satırdaki nesnenin üstünde yer yok, baloncuk altına iner.
  if (position.y === 0) {
    return { left: `${center}%`, top: `${((position.y + 1) / grid.rows) * 100}%` };
  }
  // Diğerlerinde nesnenin üstüne çıkar.
  return { left: `${center}%`, bottom: `${(1 - position.y / grid.rows) * 100}%` };
}
