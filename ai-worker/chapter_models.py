# Verinin kalıbını tanımlamak için.
from pydantic import BaseModel


# Bir işaret koşulu ya da ataması: {"name": "emily", "value": true}.
class Flag(BaseModel):
    name: str
    value: bool


# Bir göstergenin değeri ya da değişimi: {"meter": "mind", "amount": -1}.
class MeterChange(BaseModel):
    meter: str
    amount: int


# Bir seçimin oyuncuyu öldürmesi.
class Death(BaseModel):
    when: list[Flag] = []
    ending: str


# Kartı bir yöne kaydırmanın sonucu.
class Choice(BaseModel):
    label: str
    after: str = ""
    effects: list[MeterChange] = []
    set_flags: list[Flag] = []
    death: Death | None = None


# Duruma göre değişen kart metni.
class Variant(BaseModel):
    when: list[Flag]
    text: str


# Bir kart.
class Card(BaseModel):
    id: str
    time: str
    emoji: str
    who: str
    text: str
    when: list[Flag] = []
    variants: list[Variant] = []
    left: Choice
    right: Choice


# Bir gösterge. Sıfıra inerse ending ile biter.
class Meter(BaseModel):
    id: str
    emoji: str
    label: str
    ending: str


# Ölüm zincirinin bir adımı.
class ChainStep(BaseModel):
    when: list[Flag] = []
    text: str


# Bir son.
class Ending(BaseModel):
    id: str
    won: bool
    when: list[Flag] = []
    title: str
    text: str
    chain: list[ChainStep] = []
    hint: str


# Bir bölümün tamamı. Gemini'den istediğimiz kalıp bu.
class Chapter(BaseModel):
    id: str
    title: str
    vision: list[str]
    wake: str
    start: list[MeterChange]
    max_meter: int
    meters: list[Meter]
    cards: list[Card]
    endings: list[Ending]


# Listeyi sözlüğe çevirir: [Flag("emily", True)] -> {"emily": True}.
def flags_to_dict(flags):
    return {flag.name: flag.value for flag in flags}


# Sözlüğü listeye çevirir: {"emily": True} -> [Flag("emily", True)].
def dict_to_flags(values):
    return [Flag(name=name, value=value) for name, value in (values or {}).items()]


# Sözlüğü gösterge değişimlerine çevirir.
def dict_to_changes(values):
    return [MeterChange(meter=meter, amount=amount) for meter, amount in (values or {}).items()]


# Dolu olan alanları alır. Java boş alan yerine alanın hiç olmamasını bekliyor.
def without_empty(values):
    return {key: value for key, value in values.items() if value not in ("", {}, [], None)}


# Bir seçimin kural kısmı.
def choice_rules(choice):
    death = None
    if choice.death:
        death = without_empty({"when": flags_to_dict(choice.death.when), "ending": choice.death.ending})
    return without_empty(
        {
            "effects": {change.meter: change.amount for change in choice.effects},
            "set": flags_to_dict(choice.set_flags),
            "death": death,
        }
    )


# Bir seçimin metin kısmı.
def choice_text(choice):
    return without_empty({"label": choice.label, "after": choice.after})


# Chapter nesnesini Java'nın beklediği pakete çevirir: {"rules": ..., "texts": {"en": ...}}.
def to_pack(chapter):
    rules = {
        "formatVersion": 1,
        "id": chapter.id,
        "start": {change.meter: change.amount for change in chapter.start},
        "maxMeter": chapter.max_meter,
        "meters": [{"id": m.id, "emoji": m.emoji, "ending": m.ending} for m in chapter.meters],
        "cards": [
            without_empty(
                {
                    "id": card.id,
                    "time": card.time,
                    "emoji": card.emoji,
                    "when": flags_to_dict(card.when),
                    "variants": [{"when": flags_to_dict(v.when)} for v in card.variants],
                }
            )
            # Seçimin hiç etkisi olmasa da left ve right hep olmalı, bu yüzden dışarıda ekleniyor.
            | {"left": choice_rules(card.left), "right": choice_rules(card.right)}
            for card in chapter.cards
        ],
        "endings": [
            without_empty(
                {
                    "id": ending.id,
                    "won": ending.won,
                    "when": flags_to_dict(ending.when),
                }
            )
            | {"chain": [without_empty({"when": flags_to_dict(s.when)}) for s in ending.chain]}
            for ending in chapter.endings
        ],
    }
    english = {
        "title": chapter.title,
        "vision": chapter.vision,
        "wake": chapter.wake,
        "meters": {meter.id: meter.label for meter in chapter.meters},
        "cards": {
            card.id: without_empty(
                {
                    "who": card.who,
                    "text": card.text,
                    "variants": [variant.text for variant in card.variants],
                    "left": choice_text(card.left),
                    "right": choice_text(card.right),
                }
            )
            for card in chapter.cards
        },
        "endings": {
            ending.id: {
                "title": ending.title,
                "text": ending.text,
                "chain": [step.text for step in ending.chain],
                "hint": ending.hint,
            }
            for ending in chapter.endings
        },
    }
    return {"rules": rules, "texts": {"en": english}}


# Bir seçimi paketten okur.
def choice_from_pack(rules, text):
    death = rules.get("death")
    return Choice(
        label=text["label"],
        after=text.get("after", ""),
        effects=dict_to_changes(rules.get("effects")),
        set_flags=dict_to_flags(rules.get("set")),
        death=Death(when=dict_to_flags(death.get("when")), ending=death["ending"]) if death else None,
    )


# Mevcut bir bölümü Chapter nesnesine çevirir. Gemini'ye örnek göstermek için kullanıyoruz.
def from_pack(rules, english):
    cards = []
    for card in rules["cards"]:
        text = english["cards"][card["id"]]
        variants = zip(card.get("variants", []), text.get("variants", []))
        cards.append(
            Card(
                id=card["id"],
                time=card["time"],
                emoji=card["emoji"],
                who=text["who"],
                text=text["text"],
                when=dict_to_flags(card.get("when")),
                variants=[Variant(when=dict_to_flags(v["when"]), text=t) for v, t in variants],
                left=choice_from_pack(card["left"], text["left"]),
                right=choice_from_pack(card["right"], text["right"]),
            )
        )
    endings = []
    for ending in rules["endings"]:
        text = english["endings"][ending["id"]]
        steps = zip(ending["chain"], text["chain"])
        endings.append(
            Ending(
                id=ending["id"],
                won=ending["won"],
                when=dict_to_flags(ending.get("when")),
                title=text["title"],
                text=text["text"],
                chain=[ChainStep(when=dict_to_flags(s.get("when")), text=t) for s, t in steps],
                hint=text["hint"],
            )
        )
    return Chapter(
        id=rules["id"],
        title=english["title"],
        vision=english["vision"],
        wake=english["wake"],
        start=dict_to_changes(rules["start"]),
        max_meter=rules["maxMeter"],
        meters=[
            Meter(id=m["id"], emoji=m["emoji"], label=english["meters"][m["id"]], ending=m["ending"])
            for m in rules["meters"]
        ],
        cards=cards,
        endings=endings,
    )