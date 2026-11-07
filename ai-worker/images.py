# Kart fotoğraflarını üretir: Sahneleri yazar, yapay zekâya çizdirir, seçilen kareyi oyuna koyar.
#
#   python images.py scenes first-day            Eksik sahneleri Gemini yazar. Dosyayı okuyup düzeltebilirsin.
#   python images.py draw first-day              Her kart için 2 taslak çizer. Var olan taslağa dokunmaz.
#   python images.py draw first-day bus --takes 3
#   python images.py pick first-day bus=2 midnight=1@120,520,792,1024
#                                                Seçilen taslağı filtreleyip oyuna koyar. @ sonrası kırpma kutusu.

# Komut satırı için.
import argparse
# Cloudflare resmi metin olarak yolluyor.
import base64
# JSON dosyaları için.
import json
# Gizli anahtarları okumak için.
import os
# Kırpma kutusunu okumak için.
import re
# Dosya yolları için.
from pathlib import Path

# Cloudflare'e istek atmak için.
import httpx
# .env dosyasını okumak için.
from dotenv import load_dotenv
# Fotoğraf işlemek için.
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps
# Gemini cevabının kalıbı için.
from pydantic import BaseModel

# Bu dosyanın klasörü ve projenin kökü.
HERE = Path(__file__).parent
STORIES = HERE.parent / "stories"
# Kartların sahne tarifleri. Elle düzeltilebilsin diye ayrı dosyada.
SCENES = HERE / "scenes"
# Ham taslaklar. GitHub'a gitmez.
DRAFTS = HERE / "drafts" / "images"

# Cloudflare'deki çizim modeli.
FLUX_URL = "https://api.cloudflare.com/client/v4/accounts/{account}/ai/run/@cf/black-forest-labs/flux-1-schnell"
# Bütün fotoğrafların ortak tarzı. Böylece bölümler aynı albümden çıkmış gibi durur.
STYLE = (
    "Black and white documentary photograph, 1970s 35mm film, heavy film grain, high contrast, "
    "slightly underexposed. No text, no logos, no readable signs, no faces visible."
)
# Bir kart için en fazla kaç taslak. Fatura şişmesin.
MAX_TAKES = 4
# Oyundaki fotoğrafın boyutu.
SIZE = (800, 600)

# Gemini'ye sahne yazdırma görevi.
SCENE_INSTRUCTIONS = """
You describe photos for the cards of Till Midnight, a swipe survival game in the spirit of Final Destination.
Each card gets one black and white photo of the moment it shows.
For every card below, write one scene in one or two plain sentences: the place, a few concrete objects, the light.
Hint at the danger with an object, never show the death itself.
Show people only from behind, as hands, or as blurred shapes. Never show faces.
No readable text, no screens with words, and no clocks showing a time, because the image model draws them wrong.

The cards:
"""


# Gemini'nin cevabının kalıbı.
class Scene(BaseModel):
    card: str
    scene: str


class Scenes(BaseModel):
    scenes: list[Scene]


# Bölümün kurallarını okur.
def load_story(chapter):
    return json.loads((STORIES / chapter / "story.json").read_text(encoding="utf-8"))


# Bölümün sahne dosyası. Yoksa boş.
def load_scenes(chapter):
    path = SCENES / f"{chapter}.json"
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}


# Sahneleri kart sırasıyla yazar ki dosya okunaklı kalsın.
def save_scenes(chapter, scenes):
    order = [card["id"] for card in load_story(chapter)["cards"]]
    ordered = {card: scenes[card] for card in order if card in scenes}
    SCENES.mkdir(exist_ok=True)
    text = json.dumps(ordered, indent=2, ensure_ascii=False) + "\n"
    (SCENES / f"{chapter}.json").write_text(text, encoding="utf-8")


# Sahnesi olmayan kartlar için Gemini'den sahne ister. Var olanlara dokunmaz.
def write_scenes(client, chapter, asker=None):
    # Testte sahte Gemini verilebilsin. Gerçek çağrı sadece burada yüklenir.
    if asker is None:
        from gemini_client import ask as asker
    scenes = load_scenes(chapter)
    english = json.loads((STORIES / chapter / "en.json").read_text(encoding="utf-8"))["cards"]
    # Sahnesi eksik kartların kimi ve metni.
    missing = {
        card["id"]: {"who": english[card["id"]]["who"], "text": english[card["id"]]["text"]}
        for card in load_story(chapter)["cards"]
        if card["id"] not in scenes
    }
    if not missing:
        return []
    answer = asker(client, SCENE_INSTRUCTIONS + json.dumps(missing, ensure_ascii=False), Scenes)
    # Gemini'nin uydurduğu kart kimlikleri atlanır.
    added = [scene for scene in answer.scenes if scene.card in missing]
    for scene in added:
        scenes[scene.card] = scene.scene
    save_scenes(chapter, scenes)
    return [scene.card for scene in added]


# Kartlar için taslak çizer. Var olan taslağa dokunmaz, böylece yarıda kalırsa kaldığı yerden sürer.
def draw(chapter, cards, takes, post=httpx.post):
    if not 1 <= takes <= MAX_TAKES:
        raise SystemExit(f"Takes must be between 1 and {MAX_TAKES}.")
    scenes = load_scenes(chapter)
    # Sahnesi olmayan kart çizilemez.
    lost = [card for card in cards if card not in scenes]
    if lost:
        raise SystemExit(f"No scene for {', '.join(lost)}. Run: python images.py scenes {chapter}")
    # Anahtarlar sadece ortamdan okunur, hiçbir yere yazılmaz.
    url = FLUX_URL.format(account=os.environ["CLOUDFLARE_ACCOUNT_ID"])
    headers = {"Authorization": f"Bearer {os.environ['CLOUDFLARE_API_TOKEN']}"}
    folder = DRAFTS / chapter
    folder.mkdir(parents=True, exist_ok=True)
    made = []
    for card in cards:
        for take in range(1, takes + 1):
            path = folder / f"{card}-{take}.jpg"
            if path.exists():
                continue
            answer = post(url, headers=headers, json={"prompt": f"{STYLE} {scenes[card]}", "steps": 8}, timeout=120)
            body = answer.json()
            # Hata mesajı anahtar içermez, ekrana yazılabilir.
            if not body.get("success"):
                raise SystemExit(f"Cloudflare refused {card}-{take} ({answer.status_code}): {body.get('errors')}")
            path.write_bytes(base64.b64decode(body["result"]["image"]))
            made.append(path)
            print(f"drew {path.relative_to(HERE)}", flush=True)
    return made


# Ham kareyi oyundaki eski, sepya baskıya çevirir.
def develop(raw, crop=None):
    # Gerekirse önce istenen bölümü kes. Sonra ortadan 4:3 kırp ve küçült.
    image = raw.crop(crop) if crop else raw
    image = ImageOps.fit(ImageOps.grayscale(image), SIZE, Image.LANCZOS)
    # Biraz daha sert ışık.
    image = ImageEnhance.Contrast(image).enhance(1.15)
    # Film greni.
    image = Image.blend(image, Image.effect_noise(SIZE, 64), 0.08)
    # Kenarları karartan oval gölge.
    mask = Image.new("L", SIZE, 0)
    width, height = SIZE
    ImageDraw.Draw(mask).ellipse((-width * 0.05, -height * 0.05, width * 1.05, height * 1.05), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(90))
    image = Image.composite(image, ImageEnhance.Brightness(image).enhance(0.6), mask)
    # Siyahı kahveye, beyazı eski kâğıda boya.
    return ImageOps.colorize(image, black="#1a0f0c", white="#efe4cf", mid="#8a7560")


# "midnight=1@120,520,792,1024" gibi bir seçimi kart, taslak ve kırpma kutusuna ayırır.
def parse_pick(text):
    match = re.fullmatch(r"([a-z0-9-]+)=(\d+)(?:@(\d+),(\d+),(\d+),(\d+))?", text)
    if not match:
        raise SystemExit(f"Cannot read '{text}'. Write it like bus=2 or midnight=1@120,520,792,1024")
    card, take, *box = match.groups()
    crop = tuple(int(number) for number in box) if box[0] else None
    return card, int(take), crop


# Seçilen taslakları filtreleyip oyuna koyar ve kartlara fotoğraf adını yazar.
def pick(chapter, choices):
    story = load_story(chapter)
    cards = {card["id"]: card for card in story["cards"]}
    images = STORIES / chapter / "images"
    images.mkdir(exist_ok=True)
    for text in choices:
        card, take, crop = parse_pick(text)
        if card not in cards:
            raise SystemExit(f"{chapter} has no card called {card}.")
        draft = DRAFTS / chapter / f"{card}-{take}.jpg"
        if not draft.exists():
            raise SystemExit(f"Missing draft {draft.relative_to(HERE)}. Run: python images.py draw {chapter} {card}")
        develop(Image.open(draft), crop).save(images / f"{card}.jpg", quality=76, progressive=True, optimize=True)
        cards[card]["image"] = f"{card}.jpg"
        print(f"picked {card}-{take}")
    # Hikâye dosyası Java'nın beklediği biçimde, sonda boş satır olmadan yazılır.
    (STORIES / chapter / "story.json").write_text(json.dumps(story, indent=2, ensure_ascii=False), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Make photos for the cards of a chapter.")
    commands = parser.add_subparsers(dest="command", required=True)
    scenes = commands.add_parser("scenes", help="Let Gemini describe cards that have no scene yet.")
    scenes.add_argument("chapter")
    drawing = commands.add_parser("draw", help="Draw drafts with FLUX on Cloudflare.")
    drawing.add_argument("chapter")
    drawing.add_argument("cards", nargs="*", help="Only these cards. Empty means every card.")
    drawing.add_argument("--takes", type=int, default=2)
    picking = commands.add_parser("pick", help="Put chosen drafts into the game.")
    picking.add_argument("chapter")
    picking.add_argument("choices", nargs="+", help="Like bus=2 or midnight=1@120,520,792,1024")
    args = parser.parse_args()
    # Anahtarlar bu klasördeki .env dosyasından gelir.
    load_dotenv(HERE / ".env")
    if args.command == "scenes":
        # Gemini paketi sadece burada gerekir.
        from google import genai

        added = write_scenes(genai.Client(), args.chapter)
        print(f"wrote {len(added)} scenes. Read scenes/{args.chapter}.json before drawing.")
    elif args.command == "draw":
        cards = args.cards or [card["id"] for card in load_story(args.chapter)["cards"]]
        made = draw(args.chapter, cards, args.takes)
        print(f"drew {len(made)} drafts in drafts/images/{args.chapter}/")
    else:
        pick(args.chapter, args.choices)


if __name__ == "__main__":
    main()
