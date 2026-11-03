// Bu dosyanın ait olduğu paket.
package com.tensec.api.web;

// Sahne tipi.
import com.tensec.api.scene.Scene;
// Liste tipi.
import java.util.List;

// Sahnenin tarayıcıya gönderilen hali. Çözümü ele veren touches, triggers ve disasters burada yok.
public record PublicScene(
        // Sahnenin adı.
        String id,
        // Başlık.
        String title,
        // Giriş cümlesi.
        String intro,
        // Süre.
        int durationSeconds,
        // En fazla dokunuş.
        int maxTouches,
        // Izgara boyutu.
        Scene.Grid grid,
        // Nesnelerin sadece başlangıç görünüşü.
        List<PublicObject> objects) {

    // Nesnenin tarayıcıya gönderilen hali. Diğer durumları gönderilmez.
    public record PublicObject(String id, String label, Scene.Position position, String emoji) {}

    // Tam sahneden tarayıcıya gidecek hali üretir. static: nesne olmadan çağrılır.
    public static PublicScene from(Scene scene) {
        // Nesne listesini akışa çevir.
        List<PublicObject> objects = scene.objects().stream()
                // Her nesneyi herkese açık haline çevir.
                .map(PublicScene::toPublicObject)
                // Sonucu listeye topla.
                .toList();

        // Sadece güvenli alanları kopyala.
        return new PublicScene(
                scene.id(),
                scene.title(),
                scene.intro(),
                scene.durationSeconds(),
                scene.maxTouches(),
                scene.grid(),
                objects);
    }

    // Tek bir nesneyi çevirir.
    private static PublicObject toPublicObject(Scene.SceneObject object) {
        // Başlangıç durumunun emojisini bul.
        String emoji = object.states().get(object.initialState()).emoji();
        // Herkese açık nesneyi oluştur.
        return new PublicObject(object.id(), object.label(), object.position(), emoji);
    }
}