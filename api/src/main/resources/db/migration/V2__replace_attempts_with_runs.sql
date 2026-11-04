-- Oyun kart oyununa döndü: eski deneme tablosu artık kullanılmıyor.
DROP TABLE attempts;

-- Her satır oynanmış bir koşudur: oyuncu bir hikâyeye başlayıp bir sona ulaştı.
CREATE TABLE runs (
    -- Koşunun kimliği. Tarayıcı üretir, böylece aynı koşu iki kez kaydedilmez.
    run_id       UUID PRIMARY KEY,
    -- Oyuncunun kimliği. Hesap yok, tarayıcıda saklanan rastgele bir değer.
    player_id    UUID NOT NULL,
    -- Oynanan hikâye.
    story_id     TEXT NOT NULL,
    -- Ulaşılan son. Tarayıcının söylediği değil, sunucunun hesapladığı.
    ending_id    TEXT NOT NULL,
    -- Oyuncu hayatta kaldı mı?
    won          BOOLEAN NOT NULL,
    -- Kaç kart oynandı.
    cards_played INT NOT NULL,
    -- Satırın oluştuğu an.
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- İstatistik sorguları hikâyeye göre filtreler. Index bu aramayı hızlandırır.
CREATE INDEX runs_story_idx ON runs (story_id, ending_id);
