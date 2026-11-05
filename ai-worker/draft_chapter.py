# JSON dosyaları için.
import json
# Komut satırı değerleri için.
import sys
# Dosya yolları için.
from pathlib import Path

# Java'ya istek atmak için.
import httpx
# .env dosyasını okumak için.
from dotenv import load_dotenv
# Gemini paketi.
from google import genai

# Bölüm kalıbı ve çeviriler.
from chapter_models import Chapter, from_pack, to_pack

# Kullanılacak model. Yoğunsa başka bir flash modeli yaz.
MODEL = "gemini-3.5-flash"
# Java'daki kontrol adresi.
VALIDATE_URL = "http://localhost:8080/internal/validate"
# Gemini'ye en fazla kaç kez yazdıracağız. Sonsuz döngü ve fatura olmasın.
MAX_ATTEMPTS = 4
# Bir cevabı en fazla kaç saniye bekleyeceğiz.
TIMEOUT_SECONDS = 600
# Bu dosyanın klasörü ve projenin kökü.
HERE = Path(__file__).parent
STORIES = HERE.parent / "stories"

# Gemini'ye oyunun kuralları.
INSTRUCTIONS = """
You write chapters for Till Midnight, a swipe survival game in the spirit of Final Destination.
The player had a vision of a disaster, escaped it, and must survive until midnight.
Death comes back through small everyday choices that seem harmless when made.

Rules of the format:
- A chapter has 3 meters. A meter that reaches 0 ends the game with its ending.
- Cards play in order. Times start in the morning and rise toward midnight.
- Use 12 to 16 cards. Keep each card text under 30 words and each choice label under 4 words.
- A choice can set flags. A later card or death can check them with "when".
- A death's "when" checks flags from before that choice.
- Every id must be used consistently. Every ending must be reachable, and so must every card.
- At least one path must survive. Winning endings have won=true. The last winning ending has no "when".
- Death endings have a chain of 3 to 6 short steps showing how an early choice caused the death.
- Every hint tells the player what to try next, without giving the exact answer.
- The total number of paths must stay small: Most choices should not add new flags.
- Use new characters and a new setting. Do not reuse the example's story.

Here is a complete example chapter:
"""


# Bir JSON dosyasını okur.
def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


# Gemini'ye istek metnini hazırlar. Önceki deneme hatalıysa onu da ekler.
def build_prompt(brief, example, previous, errors):
    # Temel metin: Kurallar, örnek ve görev.
    prompt = f"{INSTRUCTIONS}\n{example}\n\nWrite a new chapter. Brief: {brief}\n"
    # Hata varsa düzeltmesini iste.
    if errors:
        problems = "\n".join(f"- {error}" for error in errors)
        prompt += f"\nYour previous draft:\n{previous}\n\nThe checker found these problems. Fix all of them:\n{problems}\n"
    return prompt


# Gemini'den bir bölüm ister.
def write_chapter(client, prompt):
    # İsteği gönder ve cevabın kalıba uymasını iste.
    interaction = client.interactions.create(
        model=MODEL,
        input=prompt,
        timeout=TIMEOUT_SECONDS,
        response_format={
            "type": "text",
            "mime_type": "application/json",
            "schema": Chapter.model_json_schema(),
        },
    )
    # Cevabı kontrol edip Chapter nesnesine çevir.
    return Chapter.model_validate_json(interaction.output_text)


# Bölümü Java'ya kontrol ettirir ve raporu döndürür.
def validate(chapter):
    # İsteği gönder.
    response = httpx.post(VALIDATE_URL, json=to_pack(chapter), timeout=60)
    # Cevabı sözlüğe çevir.
    report = response.json()
    # Java rapor yerine hata döndürdüyse taslağı sakla ve dur.
    if "valid" not in report:
        (HERE / "last_draft.json").write_text(chapter.model_dump_json(indent=2), encoding="utf-8")
        raise SystemExit(f"Java error {response.status_code}: {report}. Draft saved to last_draft.json")
    # Raporu döndür.
    return report


# Geçerli taslağı drafts klasörüne yazar. Oyuna girmesi senin onayınla olur.
def save_draft(chapter):
    # Paket.
    pack = to_pack(chapter)
    # Taslağın klasörü.
    folder = HERE / "drafts" / chapter.id
    folder.mkdir(parents=True, exist_ok=True)
    # Kurallar ve İngilizce metin.
    (folder / "story.json").write_text(json.dumps(pack["rules"], ensure_ascii=False, indent=2), encoding="utf-8")
    (folder / "en.json").write_text(json.dumps(pack["texts"]["en"], ensure_ascii=False, indent=2), encoding="utf-8")
    return folder


# Yaz, kontrol ettir, hatayı düzelttir döngüsü.
def draft(brief, client, write=write_chapter):
    # Örnek bölüm: first-day.
    example = from_pack(
        read_json(STORIES / "first-day" / "story.json"),
        read_json(STORIES / "first-day" / "en.json"),
    ).model_dump_json()
    # Önceki deneme ve hataları.
    previous = ""
    errors = []
    # Sınırlı sayıda dene.
    for attempt in range(1, MAX_ATTEMPTS + 1):
        print(f"Attempt {attempt}: asking Gemini, this can take a few minutes...")
        # Gemini yazar.
        chapter = write(client, build_prompt(brief, example, previous, errors))
        # Karar Java'nın.
        report = validate(chapter)
        # Geçtiyse iş bitti.
        if report["valid"]:
            print(f"Valid: {report['analysis']['paths']} paths, {report['analysis']['survivalPercent']}% survive")
            return chapter
        # Geçmediyse hataları bir sonraki denemeye taşı.
        errors = report["errors"]
        previous = chapter.model_dump_json()
        print(f"{len(errors)} problems:")
        for error in errors[:10]:
            print(f"- {error}")
    # Sınır doldu: Kötü taslak kaydetmek yerine dur.
    raise SystemExit("No valid chapter. Nothing was saved.")


# Programın başladığı yer.
def main():
    # Komutta yazılan kısa tarif.
    brief = sys.argv[1]
    # .env dosyasını yükle.
    load_dotenv()
    # Gemini istemcisi.
    client = genai.Client()
    # Taslağı üret.
    chapter = draft(brief, client)
    # Kaydet.
    print(f"Saved to {save_draft(chapter)}")


# Dosya doğrudan çalıştırılınca main çalışsın, başka dosyadan yüklenince çalışmasın.
if __name__ == "__main__":
    main()