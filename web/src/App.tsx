// React'in durum ve yan etki araçları.
import { useCallback, useEffect, useState } from "react";
// Sunucu çağrısı.
import { fetchLevels } from "./api";
// Oyun ekranı.
import { Game } from "./Game";
// Bölüm listesi ekranı.
import { LevelSelect } from "./LevelSelect";
// Tip.
import type { Level } from "./types";

// Ana bileşen: hangi ekranın görüneceğine karar verir.
export function App() {
  // Bölüm listesi. Gelene kadar null.
  const [levels, setLevels] = useState<Level[] | null>(null);
  // Oynanan sahne. null ise bölüm listesi görünür.
  const [sceneId, setSceneId] = useState<string | null>(null);
  // Sunucuya ulaşılamadı mı?
  const [failed, setFailed] = useState(false);

  // Bölüm listesini sunucudan yeniler.
  const refresh = useCallback(() => {
    // İste, gelince kaydet, olmazsa hata göster.
    fetchLevels()
      .then(setLevels)
      .catch(() => setFailed(true));
  }, []);

  // Her bölüm listesine dönüşte ilerlemeyi yenile.
  useEffect(() => {
    // Oyun sırasında gerek yok.
    if (sceneId === null) refresh();
  }, [sceneId, refresh]);

  // Sunucuya ulaşılamadıysa yönlendirme göster.
  if (failed) {
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

  // Liste daha gelmediyse bekleme yazısı göster.
  if (!levels) {
    return (
      <main className="page" data-mood="ready">
        <p className="notice">Loading levels</p>
      </main>
    );
  }

  // Bir bölüm oynanıyorsa oyun ekranını göster.
  if (sceneId) {
    // Bu bölümün sırası.
    const index = levels.findIndex((level) => level.sceneId === sceneId);
    // Sıradaki bölüm. Son bölümdeyse yok.
    const next = levels[index + 1];
    return (
      <Game
        // key: bölüm değişince oyun ekranı sıfırdan başlar.
        key={sceneId}
        sceneId={sceneId}
        onBack={() => setSceneId(null)}
        // Sıradaki bölüm varsa geçiş düğmesi çıkar.
        onNext={next ? () => setSceneId(next.sceneId) : undefined}
      />
    );
  }

  // Değilse bölüm listesini göster.
  return <LevelSelect levels={levels} onPlay={setSceneId} />;
}
