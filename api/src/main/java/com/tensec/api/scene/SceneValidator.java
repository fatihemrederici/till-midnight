// Bu dosyanın ait olduğu paket.
package com.tensec.api.scene;

// Değiştirilebilir liste.
import java.util.ArrayList;
// Değiştirilebilir anahtar-değer tablosu.
import java.util.HashMap;
// Tekrar içermeyen küme.
import java.util.HashSet;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;
// null kontrolü için yardımcı.
import java.util.Objects;
// Küme arayüzü.
import java.util.Set;
// Spring'in "bu sınıfı sen yönet" etiketi.
import org.springframework.stereotype.Component;

// Sahnenin anlam hatalarını bulur: var olmayan nesne, ızgara dışı konum gibi.
// Hataları tek tek listeler, çünkü AI bu mesajları okuyup sahneyi düzeltecek.
@Component
public class SceneValidator {

    // İzin verilen en fazla nesne. Çözücünün çalışma süresini sınırlar.
    private static final int MAX_OBJECTS = 8;
    // İzin verilen en fazla dokunuş. Çözücünün çalışma süresini sınırlar.
    private static final int MAX_TOUCHES = 3;
    // Sahne süresi şimdilik sabit.
    private static final int DURATION_SECONDS = 10;

    // Ana metot: hataların listesini döndürür. Liste boşsa sahne geçerlidir.
    public List<String> validate(Scene scene) {
        // Bulunan hatalar.
        List<String> errors = new ArrayList<>();

        // JSON'daki "null" metni null sahne üretir.
        if (scene == null) {
            // Hatayı ekle.
            errors.add("Scene is empty");
            // Devam etmenin anlamı yok.
            return errors;
        }

        // Üst seviye alanları kontrol et.
        checkSettings(scene, errors);

        // Temel listelerden biri yoksa detaylı kontrol yapamayız.
        if (scene.objects() == null || scene.touches() == null
                || scene.triggers() == null || scene.disasters() == null) {
            // Hatayı ekle.
            errors.add("objects, touches, triggers and disasters are all required");
            // Burada dur.
            return errors;
        }

        // Nesneleri kontrol et ve id'ye göre tablo oluştur.
        Map<String, Scene.SceneObject> objects = checkObjects(scene, errors);
        // Dokunuş kurallarını kontrol et.
        checkTouches(scene, objects, errors);
        // Tetikleyicileri kontrol et.
        checkTriggers(scene, objects, errors);
        // Felaketleri kontrol et.
        checkDisasters(scene, objects, errors);

        // Tüm hataları döndür.
        return errors;
    }

    // Üst seviye ayarları kontrol eder.
    private void checkSettings(Scene scene, List<String> errors) {
        // Format sürümü doğru mu?
        if (scene.formatVersion() != 1) {
            // Değilse hata ekle.
            errors.add("formatVersion must be 1");
        }
        // Sahne adı, başlık ve giriş var mı?
        if (isBlank(scene.id()) || isBlank(scene.title()) || isBlank(scene.intro())) {
            // Yoksa hata ekle.
            errors.add("id, title and intro are required");
        }
        // Süre doğru mu?
        if (scene.durationSeconds() != DURATION_SECONDS) {
            // Değilse hata ekle.
            errors.add("durationSeconds must be " + DURATION_SECONDS);
        }
        // Dokunuş sayısı sınırlar içinde mi?
        if (scene.maxTouches() < 1 || scene.maxTouches() > MAX_TOUCHES) {
            // Değilse hata ekle.
            errors.add("maxTouches must be between 1 and " + MAX_TOUCHES);
        }
        // Izgara var mı ve boyutu pozitif mi?
        if (scene.grid() == null || scene.grid().columns() < 1 || scene.grid().rows() < 1) {
            // Değilse hata ekle.
            errors.add("grid must have at least 1 column and 1 row");
        }
    }

    // Nesneleri kontrol eder ve id -> nesne tablosu döndürür.
    private Map<String, Scene.SceneObject> checkObjects(Scene scene, List<String> errors) {
        // id -> nesne tablosu. Diğer kontroller bunu kullanacak.
        Map<String, Scene.SceneObject> objects = new HashMap<>();
        // Dolu ızgara hücreleri.
        Set<Scene.Position> usedCells = new HashSet<>();

        // Nesne sayısı sınırlar içinde mi?
        if (scene.objects().isEmpty() || scene.objects().size() > MAX_OBJECTS) {
            // Değilse hata ekle.
            errors.add("A scene must have between 1 and " + MAX_OBJECTS + " objects");
        }

        // Her nesneyi dön. withoutNulls listedeki boş elemanları eler ve hata yazar.
        for (Scene.SceneObject object : withoutNulls(scene.objects(), "objects", errors)) {
            // Nesnenin adı var mı?
            if (isBlank(object.id())) {
                // Yoksa hata ekle.
                errors.add("An object has no id");
                // Sıradaki nesneye geç.
                continue;
            }
            // Tabloya ekle. put, aynı anahtar zaten varsa eski değeri döndürür.
            if (objects.put(object.id(), object) != null) {
                // Demek ki aynı adda iki nesne var.
                errors.add("Duplicate object id: " + object.id());
            }
            // Görünen adı var mı?
            if (isBlank(object.label())) {
                // Yoksa hata ekle.
                errors.add("Object " + object.id() + " has no label");
            }
            // Konumunu kontrol et.
            checkPosition(scene, object, usedCells, errors);
            // Durumlarını kontrol et.
            checkStates(object, errors);
        }
        // Tabloyu döndür.
        return objects;
    }

    // Nesnenin ızgaradaki konumunu kontrol eder.
    private void checkPosition(
            Scene scene, Scene.SceneObject object, Set<Scene.Position> usedCells, List<String> errors) {
        // Konum yazılmamış mı?
        if (object.position() == null) {
            // Hata ekle.
            errors.add("Object " + object.id() + " has no position");
            // Devam edemeyiz.
            return;
        }
        // Izgara geçersizse sınır kontrolü yapılamaz. Bu hata zaten yazıldı.
        if (scene.grid() != null) {
            // Kısa isimlerle konumu al.
            int x = object.position().x();
            // Kısa isimlerle konumu al.
            int y = object.position().y();
            // Izgaranın dışında mı?
            if (x < 0 || y < 0 || x >= scene.grid().columns() || y >= scene.grid().rows()) {
                // Hata ekle.
                errors.add("Object " + object.id() + " is outside the grid");
            }
        }
        // Küme aynı değeri kabul etmez; add false dönerse hücre doludur.
        if (!usedCells.add(object.position())) {
            // Hata ekle.
            errors.add("Object " + object.id() + " shares a cell with another object");
        }
    }

    // Nesnenin durumlarını kontrol eder.
    private void checkStates(Scene.SceneObject object, List<String> errors) {
        // Durum listesi yok ya da boş mu?
        if (object.states() == null || object.states().isEmpty()) {
            // Hata ekle.
            errors.add("Object " + object.id() + " has no states");
            // Devam edemeyiz.
            return;
        }
        // Başlangıç durumu listede var mı?
        if (!object.states().containsKey(object.initialState())) {
            // Hata ekle.
            errors.add("Object " + object.id() + ": initial state " + object.initialState() + " does not exist");
        }
        // Her durumu dön.
        for (Map.Entry<String, Scene.StateView> state : object.states().entrySet()) {
            // Emojisi var mı?
            if (state.getValue() == null || isBlank(state.getValue().emoji())) {
                // Yoksa hata ekle.
                errors.add("Object " + object.id() + ": state " + state.getKey() + " has no emoji");
            }
        }
    }

    // Dokunuş kurallarını kontrol eder.
    private void checkTouches(Scene scene, Map<String, Scene.SceneObject> objects, List<String> errors) {
        // En az bir kural şart, yoksa oyuncu hiçbir şey yapamaz.
        if (scene.touches().isEmpty()) {
            // Hata ekle.
            errors.add("A scene must have at least one touch rule");
        }
        // Her kuralı dön.
        for (Scene.TouchRule rule : withoutNulls(scene.touches(), "touches", errors)) {
            // Hata mesajlarında kullanılacak ad.
            String name = "Touch " + rule.id();
            // Dokunulan nesne var mı?
            if (!objects.containsKey(rule.object())) {
                // Yoksa hata ekle.
                errors.add(name + ": unknown object " + rule.object());
            }
            // Koşulları kontrol et.
            checkRefs(rule.when(), name, objects, errors);
            // Değişiklikleri kontrol et.
            checkEffects(rule.then(), name, objects, errors);
        }
    }

    // Tetikleyicileri kontrol eder.
    private void checkTriggers(Scene scene, Map<String, Scene.SceneObject> objects, List<String> errors) {
        // En az bir tetikleyici şart, yoksa kendiliğinden hiçbir şey olmaz.
        if (scene.triggers().isEmpty()) {
            // Hata ekle.
            errors.add("A scene must have at least one trigger");
        }
        // Her tetikleyiciyi dön.
        for (Scene.Trigger trigger : withoutNulls(scene.triggers(), "triggers", errors)) {
            // Hata mesajlarında kullanılacak ad.
            String name = "Trigger " + trigger.id();
            // at ve after'dan tam olarak biri olmalı. İkisi aynıysa ya ikisi var ya ikisi de yok.
            if ((trigger.at() == null) == (trigger.after() == null)) {
                // Hata ekle.
                errors.add(name + ": must have exactly one of at or after");
            }
            // at varsa saniyesi aralıkta mı?
            if (trigger.at() != null && (trigger.at() < 1 || trigger.at() > DURATION_SECONDS)) {
                // Değilse hata ekle.
                errors.add(name + ": at must be between 1 and " + DURATION_SECONDS);
            }
            // after varsa
            if (trigger.after() != null) {
                // Beklediği nesne ve durum var mı?
                checkRef(new Scene.StateRef(trigger.after().object(), trigger.after().state()), name, objects, errors);
                // Bekleme süresi aralıkta mı?
                if (trigger.after().delay() < 1 || trigger.after().delay() > DURATION_SECONDS) {
                    // Değilse hata ekle.
                    errors.add(name + ": delay must be between 1 and " + DURATION_SECONDS);
                }
            }
            // Koşulları kontrol et.
            checkRefs(trigger.when(), name, objects, errors);
            // Değişiklikleri kontrol et.
            checkEffects(trigger.then(), name, objects, errors);
        }
    }

    // Felaketleri kontrol eder.
    private void checkDisasters(Scene scene, Map<String, Scene.SceneObject> objects, List<String> errors) {
        // En az bir felaket şart, yoksa oyun kaybedilemez.
        if (scene.disasters().isEmpty()) {
            // Hata ekle.
            errors.add("A scene must have at least one disaster");
        }
        // Her felaketi dön.
        for (Scene.Disaster disaster : withoutNulls(scene.disasters(), "disasters", errors)) {
            // Hata mesajlarında kullanılacak ad.
            String name = "Disaster " + disaster.id();
            // Koşulsuz felaket her zaman tetiklenirdi.
            if (disaster.when() == null || disaster.when().isEmpty()) {
                // Hata ekle.
                errors.add(name + ": when is required");
            } else {
                // Koşulları kontrol et.
                checkRefs(disaster.when(), name, objects, errors);
            }
            // Oyuncuya gösterilecek cümle var mı?
            if (isBlank(disaster.caption())) {
                // Yoksa hata ekle.
                errors.add(name + ": caption is required");
            }
        }
    }

    // then listesini kontrol eder: boş olamaz.
    private void checkEffects(
            List<Scene.StateRef> effects, String name, Map<String, Scene.SceneObject> objects, List<String> errors) {
        // Değişiklik yoksa kuralın anlamı yok.
        if (effects == null || effects.isEmpty()) {
            // Hata ekle.
            errors.add(name + ": then is required");
            // Devam edemeyiz.
            return;
        }
        // İçindekileri kontrol et.
        checkRefs(effects, name, objects, errors);
    }

    // Bir "nesne + durum" listesini kontrol eder.
    private void checkRefs(
            List<Scene.StateRef> refs, String name, Map<String, Scene.SceneObject> objects, List<String> errors) {
        // Her elemanı dön.
        for (Scene.StateRef ref : withoutNulls(refs, name, errors)) {
            // Tek tek kontrol et.
            checkRef(ref, name, objects, errors);
        }
    }

    // Tek bir "nesne + durum" gerçekten var mı?
    private void checkRef(
            Scene.StateRef ref, String name, Map<String, Scene.SceneObject> objects, List<String> errors) {
        // Nesneyi tablodan bul.
        Scene.SceneObject object = objects.get(ref.object());
        // Bulunamadıysa
        if (object == null) {
            // hata ekle.
            errors.add(name + ": unknown object " + ref.object());
            // Devam edemeyiz.
            return;
        }
        // Durum listesi eksikse bu hata zaten yazıldı. Varsa durum listede olmalı.
        if (object.states() != null && !object.states().containsKey(ref.state())) {
            // Hata ekle.
            errors.add(name + ": object " + ref.object() + " has no state " + ref.state());
        }
    }

    // Listeden null elemanları çıkarır, varsa hata yazar. Liste null ise boş liste döndürür.
    private <T> List<T> withoutNulls(List<T> list, String name, List<String> errors) {
        // Liste yoksa dönülecek bir şey de yok.
        if (list == null) {
            // Boş liste döndür.
            return List.of();
        }
        // Null olmayanları topla.
        List<T> filled = list.stream().filter(Objects::nonNull).toList();
        // Boyut küçüldüyse null eleman vardı.
        if (filled.size() != list.size()) {
            // Hata ekle.
            errors.add(name + ": contains an empty item");
        }
        // Temiz listeyi döndür.
        return filled;
    }

    // Metin yok ya da sadece boşluk mu?
    private boolean isBlank(String text) {
        // isBlank boşlukları da boş sayar.
        return text == null || text.isBlank();
    }
}