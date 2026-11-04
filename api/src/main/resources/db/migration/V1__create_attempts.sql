-- Her satır bir denemedir. Dosya adındaki V1 sürüm numarasıdır, Flyway dosyaları sırayla çalıştırır.
CREATE TABLE attempts (
    -- Denemenin kimliği. Tarayıcı üretir, böylece aynı deneme iki kez kaydedilmez.
    attempt_id  UUID PRIMARY KEY,
    -- Oyuncunun kimliği. Hesap yok, tarayıcıda saklanan rastgele bir değer.
    player_id   UUID NOT NULL,
    -- Oynanan sahne.
    scene_id    TEXT NOT NULL,
    -- Oynandığı gün. Sahneler döndüğü için istatistikler güne göre ayrılır.
    played_on   DATE NOT NULL,
    -- Kaç dokunuş kullanıldı.
    touch_count INT NOT NULL,
    -- Kazanıldı mı?
    won         BOOLEAN NOT NULL,
    -- Döngü kaçıncı saniyede bitti.
    end_second  INT NOT NULL,
    -- Hangi felaket oldu. Kazanıldıysa boş.
    disaster_id TEXT,
    -- Satırın oluştuğu an.
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- İstatistik sorguları hep sahne ve güne göre filtreler. Index bu aramayı hızlandırır.
CREATE INDEX attempts_scene_day_idx ON attempts (scene_id, played_on);