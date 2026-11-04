// Tip.
import type { Level } from "./types";

// LevelSelect bileşeninin dışarıdan aldıkları.
type LevelSelectProps = {
  // Gösterilecek bölümler.
  levels: Level[];
  // Bölüme dokununca çalışır.
  onPlay: (sceneId: string) => void;
};

// Bölüm listesi ekranı.
export function LevelSelect({ levels, onPlay }: LevelSelectProps) {
  // Yıldız sayısı.
  const stars = levels.filter((level) => level.star).length;

  // Ekranı çiz.
  return (
    <main className="page" data-mood="ready">
      <header className="top">
        <div>
          <p className="brand">10sec</p>
          <h1 className="title">Levels</h1>
        </div>
        {/* Toplanan yıldızlar. */}
        <p className="star-count">
          ⭐ {stars}/{levels.length}
        </p>
      </header>

      {/* Bölümler sırayla. */}
      <ol className="levels">
        {levels.map((level) => (
          <li key={level.sceneId}>
            <button
              className="level"
              // Kilitli bölüme dokunulamaz.
              disabled={!level.unlocked}
              data-locked={!level.unlocked}
              onClick={() => onPlay(level.sceneId)}
            >
              <span className="level-number">{level.number}</span>
              {/* Kilitli bölümün adı gizli: oyuncu neyin geleceğini merak etsin. */}
              <span className="level-title">{level.unlocked ? level.title : "Locked"}</span>
              {/* Durum işareti. */}
              <span className="level-status" aria-hidden="true">
                {!level.unlocked ? "🔒" : level.star ? "⭐" : level.completed ? "✅" : ""}
              </span>
            </button>
          </li>
        ))}
      </ol>
    </main>
  );
}
