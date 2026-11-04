// Oyun mantığı.
import { useGame } from "./useGame";

// Ana bileşen: oyunun tüm ekranı.
export function App() {
  // Oyun durumunu ve işlemleri al.
  const game = useGame();
  // Kısa isimlerle kullanmak için.
  const { scene, phase, second, touches, attempt } = game;

  // Sunucuya ulaşılamadıysa yönlendirme göster.
  if (phase === "error") {
    return (
      <main className="page" data-mood="lost">
        <h1 className="title">Can't reach the game server</h1>
        <p className="notice">
          Start it in another terminal with <code>./mvnw spring-boot:run</code>, then reload this page.
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
  const canTouch = phase === "playing" && !game.touchedThisLoop && touchesLeft > 0;
  // Saniye çizgisindeki kutular: 0'dan 9'a.
  const seconds = Array.from({ length: scene.durationSeconds }, (_, index) => index);

  // Ekranı çiz. data-mood arka plan rengini belirler.
  return (
    <main className="page" data-mood={phase}>
      <header className="top">
        <div>
          <p className="brand">10sec</p>
          <h1 className="title">{scene.title}</h1>
        </div>
        {/* Büyük saniye sayacı. */}
        <p className="clock">
          {second}
        </p>
      </header>

      {/* Sahne ızgarası. Boyutu sahneden gelir. */}
      <section
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
      </section>

      {/* 10 saniyelik zaman çizgisi. */}
      <ol className="timeline" aria-label="Seconds">
        {seconds.map((value) => (
          <li
            key={value}
            className="tick"
            // Geçmiş saniyeler dolu görünür.
            data-past={phase !== "ready" && value < second}
            // Şu anki saniye vurgulanır.
            data-now={phase === "playing" && value === second}
            // Dokunuş yapılan saniye işaretlenir.
            data-touched={touches.some((touch) => touch.second === value)}
          >
            {value}
          </li>
        ))}
      </ol>

      {/* Alt kısım: aşamaya göre değişir. aria-live: ekran okuyucular yeni cümleyi okur. */}
      <section className="panel" aria-live="polite">
        {phase === "ready" && (
          <>
            <p className="caption">{scene.intro}</p>
            <p className="hint">
              Something goes wrong within 10 seconds. Touch one thing per try, and time rewinds until you stop it.
            </p>
            <button className="action" onClick={() => game.startLoop([])}>
              Start
            </button>
          </>
        )}

        {phase === "playing" && (
          <>
            <p className="caption">{game.caption ?? scene.intro}</p>
            <p className="hint">
              {canTouch
                ? "Touch something at the right moment."
                : touchesLeft === 0
                  ? "No touches left. Watch what happens."
                  : "Touch made. Watch what it changes."}
            </p>
          </>
        )}

        {phase === "lost" && (
          <>
            <p className="caption">{attempt?.disasterCaption}</p>
            <div className="actions">
              <button className="action" onClick={game.rewind}>
                {touchesLeft === 0 ? "Undo last touch and rewind" : "Rewind"}
              </button>
              {touches.length > 0 && (
                <button className="action quiet" onClick={game.startOver}>
                  Clear touches
                </button>
              )}
            </div>
          </>
        )}

        {phase === "won" && (
          <>
            <p className="caption">Nothing went wrong. Nobody will ever know.</p>
            <p className="hint">
              Solved on try {game.attemptCount} with {touches.length} {touches.length === 1 ? "touch" : "touches"}.{" "}
              {attempt?.bestTouches === touches.length
                ? "That is the fewest possible."
                : `It can be done with ${attempt?.bestTouches}.`}
            </p>
            {game.stats && (
              <p className="hint">
                {game.stats.solvers} of {game.stats.players} {game.stats.players === 1 ? "player" : "players"} solved
                this scene today.
              </p>
            )}
            <button className="action" onClick={game.startOver}>
              Play again
            </button>
          </>
        )}

        {/* Dokunuş hakkı göstergesi. Oyun başlayınca görünür. */}
        {phase !== "ready" && (
          <p className="meta">
            Try {game.attemptCount}, {touchesLeft} {touchesLeft === 1 ? "touch" : "touches"} left
          </p>
        )}
      </section>
    </main>
  );
}