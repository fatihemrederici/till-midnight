// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Motorun sonuç tipi.
import com.tensec.api.engine.SimulationResult;
// Motorun olay tipi.
import com.tensec.api.engine.TimelineEvent;
// Sahne tipi.
import com.tensec.api.scene.Scene;
// Liste tipi.
import java.util.List;

// POST /attempt cevabı: web tarafının animasyonu oynatmak için ihtiyaç duyduğu her şey.
public record AttemptResponse(
        // Oyuncu kazandı mı?
        boolean won,
        // Döngü kaçıncı saniyede bitti?
        int endSecond,
        // Felaketin ekranda gösterilecek cümlesi. Kazandıysa null.
        String disasterCaption,
        // Sırayla oynatılacak olaylar.
        List<EventView> events) {

    // Tek bir olayın ekrana yansıyan hali. Kural adını göndermiyoruz, çünkü oyuncunun işine yaramaz.
    public record EventView(int second, String object, String emoji, String caption, boolean noEffect) {}

    // Motorun sonucundan cevabı üretir.
    public static AttemptResponse from(Scene scene, SimulationResult result) {
        // Olay listesini akışa çevir.
        List<EventView> events = result.events().stream()
                // Her olayı ekran haline çevir. e -> ... Swift'teki closure gibi.
                .map(event -> toView(scene, event))
                // Sonucu listeye topla.
                .toList();

        // Cevabı oluştur.
        return new AttemptResponse(result.won(), result.endSecond(), disasterCaption(scene, result), events);
    }

    // Tek bir olayı çevirir.
    private static EventView toView(Scene scene, TimelineEvent event) {
        // Motor, etkisiz dokunuşlarda ruleId'yi null bırakır.
        boolean noEffect = event.ruleId() == null;
        // Yeni durumun emojisini bul.
        String emoji = emojiOf(scene, event.object(), event.to());
        // Ekran halini oluştur.
        return new EventView(event.second(), event.object(), emoji, event.caption(), noEffect);
    }

    // Bir nesnenin verilen durumdaki emojisini bulur.
    private static String emojiOf(Scene scene, String objectId, String state) {
        // Tüm nesneleri dön.
        for (Scene.SceneObject object : scene.objects()) {
            // Aradığımız nesne bu mu?
            if (object.id().equals(objectId)) {
                // Durumunun emojisini döndür.
                return object.states().get(state).emoji();
            }
        }
        // Motor sadece sahnedeki nesneleri ürettiği için buraya gelinmemesi gerekir.
        throw new IllegalStateException("Unknown object: " + objectId);
    }

    // Felaketin cümlesini bulur.
    private static String disasterCaption(Scene scene, SimulationResult result) {
        // Tüm felaketleri dön.
        for (Scene.Disaster disaster : scene.disasters()) {
            // Olan felaket bu mu? disaster.id() önde, çünkü sonuçtaki id null olabilir.
            if (disaster.id().equals(result.disasterId())) {
                // Cümlesini döndür.
                return disaster.caption();
            }
        }
        // Felaket yoksa cümle de yok.
        return null;
    }
}