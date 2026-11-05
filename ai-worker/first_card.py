# .env dosyasını okumak için.
from dotenv import load_dotenv
# Gemini'nin Python paketi.
from google import genai
# Verinin kalıbını tanımlamak için.
from pydantic import BaseModel

# Kullanılacak model.
MODEL = "gemini-3.5-flash"

# Gemini'ye verilen görev.
PROMPT = """
You write cards for a swipe survival game in the spirit of Final Destination.
The player is a night nurse who saw a disaster in a vision.
Write one tense card with a small everyday choice.
Keep the text under 25 words and each choice label under 4 words.
"""


# Beklediğimiz kartın kalıbı. Swift'teki Codable struct gibi.
class Card(BaseModel):
    # Kartta konuşan kişi.
    who: str
    # Kartın metni.
    text: str
    # Sola kaydırma seçeneği.
    left_label: str
    # Sağa kaydırma seçeneği.
    right_label: str


# Programın başladığı yer.
def main():
    # .env içindeki GEMINI_API_KEY değerini yükle.
    load_dotenv()
    # Gemini istemcisi, anahtarı kendisi bulur.
    client = genai.Client()
    # İsteği gönder ve cevabın kalıba uymasını iste.
    interaction = client.interactions.create(
        model=MODEL,
        input=PROMPT,
        response_format={
            "type": "text",
            "mime_type": "application/json",
            "schema": Card.model_json_schema(),
        },
    )
    # Gelen JSON'u kontrol edip Card nesnesine çevir. Kalıba uymazsa burada hata verir.
    card = Card.model_validate_json(interaction.output_text)
    # Kartı yazdır.
    print(card)


# Programı çalıştır.
main()