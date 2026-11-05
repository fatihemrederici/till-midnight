# JSON dosyalarını okumak için.
import json
# Komut satırına yazılan değeri almak için.
import sys
# Dosya yollarıyla çalışmak için.
from pathlib import Path

# Java sunucusuna istek atmak için.
import httpx

# Java'daki hikâye kontrol adresi.
VALIDATE_URL = "http://localhost:8080/internal/validate"
# Oyunun dilleri.
LANGUAGES = ["en", "tr", "pl"]
# Bu dosyanın bir üst klasöründeki stories klasörü.
STORIES = Path(__file__).parent.parent / "stories"


# Bir JSON dosyasını okuyup Python sözlüğüne çevirir.
def read_json(path):
    # Türkçe ve Lehçe harfler bozulmasın diye UTF-8.
    return json.loads(path.read_text(encoding="utf-8"))


# Bir bölümün kurallarını ve dillerini tek pakette toplar.
def load_pack(chapter):
    # Bölümün klasörü, örneğin stories/first-day.
    folder = STORIES / chapter
    # Java'nın beklediği biçim: {"rules": ..., "texts": {"en": ..., "tr": ..., "pl": ...}}.
    return {
        "rules": read_json(folder / "story.json"),
        "texts": {language: read_json(folder / f"{language}.json") for language in LANGUAGES},
    }


# Programın başladığı yer.
def main():
    # Komutta yazılan bölüm adı, örneğin first-day.
    chapter = sys.argv[1]
    # Paketi JSON olarak Java'ya gönder.
    response = httpx.post(VALIDATE_URL, json=load_pack(chapter))
    # Cevabı sözlüğe çevir.
    report = response.json()

    # Bölüm geçerliyse yol sayısını yaz.
    if report["valid"]:
        print(f"{chapter} is valid: {report['analysis']['paths']} paths")
    # Değilse hataları tek tek yaz.
    else:
        print(f"{chapter} has problems:")
        for error in report["errors"]:
            print(f"- {error}")


# Programı çalıştır.
main()