-- Hikâyenin kuralları değişince eski koşular başka bir oyunun sonucudur.
-- Bu yüzden her koşu, hangi sürümle oynandığını da saklar. Eski kayıtların sürümü bilinmiyor.
ALTER TABLE runs ADD COLUMN story_version TEXT NOT NULL DEFAULT 'unknown';
