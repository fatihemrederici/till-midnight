#!/usr/bin/env bash
# Oyunu tek komutla başlatır: veritabanı, sunucu ve oyun. Ctrl + C hepsini kapatır.
set -e
# Komut nereden çalıştırılırsa çalıştırılsın proje klasörüne geç.
cd "$(dirname "$0")"

# Docker kapalıysa aç ve hazır olmasını bekle.
if ! docker info >/dev/null 2>&1; then
  echo "Docker Desktop açılıyor..."
  open -a Docker
  until docker info >/dev/null 2>&1; do sleep 2; done
fi

# Veritabanı arka planda çalışır.
docker compose up -d db

# Çıkarken başlattığımız her şeyi kapat.
trap 'kill 0' EXIT

# Java sunucusu.
(cd api && ./mvnw -q spring-boot:run) &

# Web oyunu. Paketler yoksa önce indir.
(
  cd web
  if [ ! -d node_modules ]; then npm install; fi
  npm run dev
) &

# Sunucu cevap verince tarayıcıyı aç.
echo "Sunucu başlıyor..."
until curl -s -o /dev/null http://localhost:8080/stories; do sleep 2; done
open http://localhost:5173
echo "Oyun açık: http://localhost:5173  (kapatmak için Ctrl + C)"

# Sunucu ve oyun çalıştığı sürece bekle.
wait
