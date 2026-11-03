// Bu dosyanın ait olduğu paket.
package com.tensec.api.scene;

// Sıralı liste tipi. Swift'teki Array gibi.
import java.util.List;
// Anahtar-değer tipi. Swift'teki Dictionary gibi.
import java.util.Map;

// Sahne JSON dosyasının Java karşılığı. Kurallar için docs/scene-format.md dosyasına bak.
// touches, triggers ve disasters bulmacanın çözümünü içerir, bu yüzden tarayıcıya asla gönderilmez.
// record: sadece veri tutan, değiştirilemeyen sınıf. Swift'teki Codable struct gibi düşün.
public record Scene(
        // Format sürümü. İleride format değişirse eski dosyaları ayırt etmek için.
        int formatVersion,
        // Sahnenin benzersiz adı, örneğin "cafe-candle".
        String id,
        // Oyuncunun göreceği başlık.
        String title,
        // Oyun başında gösterilen kısa giriş cümlesi.
        String intro,
        // Sahnenin süresi. Şimdilik hep 10.
        int durationSeconds,
        // Oyuncunun bir sahnede biriktirebileceği en fazla dokunuş sayısı.
        int maxTouches,
        // Ekrandaki ızgaranın boyutu.
        Grid grid,
        // Sahnedeki nesneler: kedi, mum gibi.
        List<SceneObject> objects,
        // Oyuncu dokununca çalışan kurallar.
        List<TouchRule> touches,
        // Kendiliğinden çalışan kurallar.
        List<Trigger> triggers,
        // Döngüyü kaybettiren durumlar.
        List<Disaster> disasters) {

    // Izgaranın sütun ve satır sayısı.
    public record Grid(int columns, int rows) {}

    // Nesnenin ızgaradaki yeri. Sayım 0'dan başlar.
    public record Position(int x, int y) {}

    // Bir durumun ekranda nasıl görüneceği. Şimdilik sadece emoji.
    public record StateView(String emoji) {}

    // Sahnedeki tek bir nesne.
    public record SceneObject(
            // Kodun nesneyi tanıdığı ad, örneğin "candle".
            String id,
            // Oyuncunun göreceği ad, örneğin "Candle".
            String label,
            // Izgaradaki yeri.
            Position position,
            // 0. saniyedeki durumu.
            String initialState,
            // Olabileceği tüm durumlar: durum adı -> görünüşü.
            Map<String, StateView> states) {}

    // "Şu nesne şu durumda" bilgisi. when içinde koşul, then içinde değişiklik anlamına gelir.
    public record StateRef(String object, String state) {}

    // Oyuncunun dokunuşunun ne yapacağını anlatan kural.
    public record TouchRule(
            // Kuralın adı.
            String id,
            // Dokunulan nesne.
            String object,
            // Kuralın çalışması için doğru olması gereken koşullar.
            List<StateRef> when,
            // Kural çalışınca değişecek durumlar.
            List<StateRef> then,
            // Ekranda gösterilecek açıklama cümlesi.
            String caption) {

        // Compact constructor: nesne oluşurken çalışan kısım.
        public TouchRule {
            // JSON'da when yoksa null yerine boş liste koy; varsa değiştirilemez kopyasını al.
            when = when == null ? List.of() : List.copyOf(when);
        }
    }

    // Kendiliğinden çalışan kural. Ya belli bir saniyede (at) ya da bir durumdan sonra (after) çalışır.
    public record Trigger(
            // Kuralın adı.
            String id,
            // Çalışacağı saniye. Integer çünkü boş (null) olabilir; Swift'teki Int? gibi.
            Integer at,
            // Hangi durumdan kaç saniye sonra çalışacağı. Bu da boş olabilir.
            After after,
            // Çalışma anında doğru olması gereken koşullar.
            List<StateRef> when,
            // Çalışınca değişecek durumlar.
            List<StateRef> then,
            // Ekranda gösterilecek açıklama cümlesi.
            String caption) {

        // Nesne oluşurken çalışan kısım.
        public Trigger {
            // JSON'da when yoksa null yerine boş liste koy.
            when = when == null ? List.of() : List.copyOf(when);
        }
    }

    // "Şu nesne şu duruma girdikten delay saniye sonra" bilgisi. Zincirleri bu kurar.
    public record After(String object, String state, int delay) {}

    // Kaybetme koşulu: when içindekilerin hepsi doğruysa döngü biter.
    public record Disaster(String id, List<StateRef> when, String caption) {}
}
