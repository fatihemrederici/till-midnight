# Sahte cevaplar üretmek için.
import base64
import io
import json
import shutil
from pathlib import Path
from types import SimpleNamespace

# Testi çalıştıran araç.
import pytest
# Sahte fotoğraf için.
from PIL import Image

# Test edilen araç.
import images

# Projedeki hikâyeler.
STORIES = Path(__file__).parent.parent.parent / "stories"


# Küçük, gri bir JPEG.
def fake_jpeg(size=(64, 64)):
    buffer = io.BytesIO()
    Image.new("RGB", size, "gray").save(buffer, "JPEG")
    return buffer.getvalue()


# Her test kendi kopyası üzerinde çalışır, gerçek hikâye bozulmaz.
@pytest.fixture
def world(tmp_path, monkeypatch):
    shutil.copytree(STORIES / "first-day", tmp_path / "stories" / "first-day")
    monkeypatch.setattr(images, "STORIES", tmp_path / "stories")
    monkeypatch.setattr(images, "SCENES", tmp_path / "scenes")
    monkeypatch.setattr(images, "DRAFTS", tmp_path / "drafts")
    monkeypatch.setattr(images, "HERE", tmp_path)
    monkeypatch.setenv("CLOUDFLARE_ACCOUNT_ID", "account")
    monkeypatch.setenv("CLOUDFLARE_API_TOKEN", "token")
    return tmp_path


# Yayındaki bölümün her kartının sahnesi olmalı ki fotoğraflar yeniden çizilebilsin.
def test_every_first_day_card_has_a_scene():
    cards = [card["id"] for card in images.load_story("first-day")["cards"]]
    assert list(images.load_scenes("first-day")) == cards


# Filtre, ne boyda gelirse gelsin oyunun boyunda renkli bir baskı vermeli.
def test_develop_makes_a_photoof_the_game_size():
    photo = images.develop(Image.new("RGB", (1024, 1024), "white"), crop=(0, 0, 800, 700))
    assert photo.size == (800, 600)
    assert photo.mode == "RGB"


# Seçimler kart, taslak ve kırpma kutusuna ayrılmalı.
def test_reads_picks():
    assert images.parse_pick("bus=2") == ("bus", 2, None)
    assert images.parse_pick("midnight=1@120,520,792,1024") == ("midnight", 1, (120, 520, 792, 1024))
    with pytest.raises(SystemExit):
        images.parse_pick("bus 2")


# Var olan taslak yeniden çizilmemeli: Hem zaman hem para.
def test_draw_skips_drafts_that_exist(world):
    images.save_scenes("first-day", {"bus": "A bus."})
    (world / "drafts" / "first-day").mkdir(parents=True)
    (world / "drafts" / "first-day" / "bus-1.jpg").write_bytes(fake_jpeg())
    calls = []

    def post(url, headers, json, timeout):
        calls.append(json["prompt"])
        image = base64.b64encode(fake_jpeg()).decode()
        return SimpleNamespace(status_code=200, json=lambda: {"success": True, "result": {"image": image}})

    made = images.draw("first-day", ["bus"], 2, post=post)
    assert [path.name for path in made] == ["bus-2.jpg"]
    assert calls == [f"{images.STYLE} A bus."]


# Cloudflare reddederse araç durmalı ve sebebini söylemeli.
def test_draw_stops_when_cloudflare_refuses(world):
    images.save_scenes("first-day", {"bus": "A bus."})

    def post(url, headers, json, timeout):
        return SimpleNamespace(status_code=401, json=lambda: {"success": False, "errors": ["Invalid token"]})

    with pytest.raises(SystemExit, match="Invalid token"):
        images.draw("first-day", ["bus"], 1, post=post)


# Sahnesi olmayan kart çizilmeye çalışılmamalı.
def test_draw_needs_a_scene(world):
    with pytest.raises(SystemExit, match="No scene for bus"):
        images.draw("first-day", ["bus"], 1, post=None)


# Gemini sadece eksik kartları yazmalı, uydurduğu kartlar atlanmalı.
def test_scenes_fill_only_missing_cards(world):
    story = images.load_story("first-day")
    known = {card["id"]: "Old scene." for card in story["cards"][1:]}
    images.save_scenes("first-day", known)
    first = story["cards"][0]["id"]

    def asker(client, prompt, model):
        assert first in prompt and story["cards"][1]["id"] not in prompt
        return model(scenes=[{"card": first, "scene": "New scene."}, {"card": "ghost", "scene": "Nope."}])

    assert images.write_scenes(None, "first-day", asker) == [first]
    scenes = images.load_scenes("first-day")
    assert scenes[first] == "New scene." and "ghost" not in scenes
    assert list(scenes)[0] == first


# Seçilen taslak oyuna girmeli ve karta adı yazılmalı.
def test_pick_puts_the_photo_on_the_card(world):
    (world / "drafts" / "first-day").mkdir(parents=True)
    (world / "drafts" / "first-day" / "bus-2.jpg").write_bytes(fake_jpeg((1024, 1024)))
    story_file = world / "stories" / "first-day" / "story.json"
    story = json.loads(story_file.read_text(encoding="utf-8"))
    del story["cards"][0]["image"]
    story_file.write_text(json.dumps(story, indent=2, ensure_ascii=False), encoding="utf-8")

    images.pick("first-day", ["bus=2"])

    bus = json.loads(story_file.read_text(encoding="utf-8"))["cards"][0]
    assert bus["image"] == "bus.jpg"
    assert Image.open(world / "stories" / "first-day" / "images" / "bus.jpg").size == (800, 600)


# Taslağı olmayan seçim durmalı.
def test_pick_needs_a_draft(world):
    with pytest.raises(SystemExit, match="Missing draft"):
        images.pick("first-day", ["bus=3"])
