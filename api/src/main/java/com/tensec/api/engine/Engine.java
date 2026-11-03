// Bu dosyanın ait olduğu paket.
package com.tensec.api.engine;

// Sahne tipini başka paketten içeri alıyoruz.
import com.tensec.api.scene.Scene;
// Değiştirilebilir liste.
import java.util.ArrayList;
// Sıralama kuralı yazmaya yarayan tip.
import java.util.Comparator;
// Değiştirilebilir anahtar-değer tablosu. Swift'teki Dictionary gibi.
import java.util.HashMap;
// Tekrar içermeyen küme. Swift'teki Set gibi.
import java.util.HashSet;
// Liste arayüzü.
import java.util.List;
// Anahtar-değer arayüzü.
import java.util.Map;

// Sahneyi saniye saniye çalıştıran motor.
// Deterministik: rastgelelik yok, aynı girdi her zaman aynı sonucu verir.
public class Engine {

    // Ana metot: sahneyi ve dokunuşları alır, sonucu döndürür.
    public SimulationResult simulate(Scene scene, List<Touch> touches) {
        // Önce gelen dokunuşlar kurallara uyuyor mu diye kontrol et.
        validate(scene, touches);

        // Her nesnenin şu anki durumu: "cat" -> "asleep".
        Map<String, String> states = new HashMap<>();
        // Her nesnenin o duruma girdiği saniye: "cat" -> 0.
        Map<String, Integer> enteredAt = new HashMap<>();
        // Olan her şeyin sırayla tutulduğu liste.
        List<TimelineEvent> events = new ArrayList<>();

        // Sahnedeki her nesneyi dön.
        for (Scene.SceneObject object : scene.objects()) {
            // Nesnenin başlangıç durumunu yaz.
            states.put(object.id(), object.initialState());
            // Bu duruma 0. saniyede girdi.
            enteredAt.put(object.id(), 0);
        }

        // Gelen listeyi bozmamak için kopyasını al.
        List<Touch> sortedTouches = new ArrayList<>(touches);
        // Nesne adına göre sırala; oyuncunun dokunma sırası sonucu değiştirmesin.
        sortedTouches.sort(Comparator.comparing(Touch::object));

        // Zamanı 0'dan sahne süresine kadar saniye saniye ilerlet.
        for (int second = 0; second <= scene.durationSeconds(); second++) {
            // Bu saniye çalışacak tetikleyicilerin listesi.
            // Önce listeyi çıkarıyoruz ki aynı duruma bağlı tetikleyicilerin hepsi çalışsın.
            List<Scene.Trigger> dueTriggers = new ArrayList<>();
            // Tüm tetikleyicileri dön.
            for (Scene.Trigger trigger : scene.triggers()) {
                // Zamanı geldi mi?
                if (isDue(trigger, second, states, enteredAt)) {
                    // Geldiyse listeye ekle.
                    dueTriggers.add(trigger);
                }
            }

            // Zamanı gelen tetikleyicileri dosyadaki sırayla dön.
            for (Scene.Trigger trigger : dueTriggers) {
                // Koşulları şu an doğru mu?
                if (conditionsHold(trigger.when(), states)) {
                    // Doğruysa durumları değiştir ve olayı kaydet.
                    applyEffects(trigger.then(), trigger.id(), trigger.caption(), second, states, enteredAt, events);
                }
            }

            // Tüm dokunuşları dön.
            for (Touch touch : sortedTouches) {
                // Bu dokunuş bu saniyeye mi ait?
                if (touch.second() == second) {
                    // Aitse uygula.
                    applyTouch(scene, touch, states, enteredAt, events);
                }
            }

            // Tüm felaketleri dosyadaki sırayla dön.
            for (Scene.Disaster disaster : scene.disasters()) {
                // Felaketin koşulları oluştu mu?
                if (conditionsHold(disaster.when(), states)) {
                    // Oluştuysa oyuncu kaybetti, döngüyü bitir.
                    return new SimulationResult(false, second, disaster.id(), events);
                }
            }
        }

        // Süre felaketsiz bittiyse oyuncu kazandı.
        return new SimulationResult(true, scene.durationSeconds(), null, events);
    }

    // Dokunuş listesi geçerli mi diye kontrol eder, değilse hata fırlatır.
    private void validate(Scene scene, List<Touch> touches) {
        // Sahnenin izin verdiğinden fazla dokunuş var mı?
        if (touches.size() > scene.maxTouches()) {
            // Varsa isteği reddet.
            throw new IllegalArgumentException("Too many touches: at most " + scene.maxTouches() + " allowed");
        }
        // Küme tekrarları atar; boyut küçüldüyse aynı dokunuş iki kere gelmiş demektir.
        if (new HashSet<>(touches).size() != touches.size()) {
            // Tekrarı reddet.
            throw new IllegalArgumentException("Duplicate touch");
        }
        // Her dokunuşu tek tek kontrol et.
        for (Touch touch : touches) {
            // Saniye 0 ile süre-1 arasında mı?
            if (touch.second() < 0 || touch.second() >= scene.durationSeconds()) {
                // Değilse reddet.
                throw new IllegalArgumentException("Touch second out of range: " + touch.second());
            }
            // Bu adda bir nesne sahnede var mı? o -> ... Swift'teki closure gibi.
            boolean objectExists = scene.objects().stream().anyMatch(o -> o.id().equals(touch.object()));
            // Yoksa
            if (!objectExists) {
                // reddet.
                throw new IllegalArgumentException("Unknown object: " + touch.object());
            }
        }
    }

    // Tek bir dokunuşu uygular.
    private void applyTouch(
            // Kuralların bulunduğu sahne.
            Scene scene,
            // Uygulanacak dokunuş.
            Touch touch,
            // Nesnelerin şu anki durumları.
            Map<String, String> states,
            // Nesnelerin duruma giriş saniyeleri.
            Map<String, Integer> enteredAt,
            // Olay listesi.
            List<TimelineEvent> events) {
        // Dokunuş kurallarını sırayla dön.
        for (Scene.TouchRule rule : scene.touches()) {
            // Kural bu nesne için mi ve koşulları doğru mu?
            if (rule.object().equals(touch.object()) && conditionsHold(rule.when(), states)) {
                // Kuralı uygula.
                applyEffects(rule.then(), rule.id(), rule.caption(), touch.second(), states, enteredAt, events);
                // İlk uyan kural yeter, metottan çık.
                return;
            }
        }
        // Buraya geldiysek hiçbir kural uymadı. Nesnenin mevcut durumunu al.
        String current = states.get(touch.object());
        // Etkisiz dokunuşu kaydet: from ve to aynı, kural yok.
        events.add(new TimelineEvent(touch.second(), touch.object(), current, current, null, null));
    }

    // Bu tetikleyicinin bu saniyede çalışma zamanı geldi mi?
    private boolean isDue(
            Scene.Trigger trigger, int second, Map<String, String> states, Map<String, Integer> enteredAt) {
        // Tetikleyici sabit saniyeli mi (at)?
        if (trigger.at() != null) {
            // O saniyedeysek zamanı gelmiştir.
            return trigger.at() == second;
        }
        // Değilse after tipindedir.
        Scene.After after = trigger.after();
        // Nesne hâlâ o durumda mı ve tam delay saniye önce mi girdi? Zinciri kıran kontrol bu.
        return states.get(after.object()).equals(after.state())
                && enteredAt.get(after.object()) == second - after.delay();
    }

    // Verilen koşulların hepsi doğru mu?
    private boolean conditionsHold(List<Scene.StateRef> conditions, Map<String, String> states) {
        // Her koşulu dön.
        for (Scene.StateRef condition : conditions) {
            // Nesnenin durumu beklenenden farklı mı? Metinler == ile değil equals ile karşılaştırılır.
            if (!states.get(condition.object()).equals(condition.state())) {
                // Biri bile yanlışsa cevap false.
                return false;
            }
        }
        // Hepsi doğruysa (ya da hiç koşul yoksa) cevap true.
        return true;
    }

    // then listesindeki değişiklikleri uygular ve olayları kaydeder.
    private void applyEffects(
            // Uygulanacak değişiklikler.
            List<Scene.StateRef> effects,
            // Bu değişikliğe sebep olan kuralın adı.
            String ruleId,
            // Ekranda gösterilecek cümle.
            String caption,
            // Şu anki saniye.
            int second,
            // Nesnelerin durumları.
            Map<String, String> states,
            // Nesnelerin duruma giriş saniyeleri.
            Map<String, Integer> enteredAt,
            // Olay listesi.
            List<TimelineEvent> events) {
        // Her değişikliği dön.
        for (Scene.StateRef effect : effects) {
            // Nesnenin şu anki durumunu al.
            String current = states.get(effect.object());
            // Zaten o durumdaysa
            if (current.equals(effect.state())) {
                // hiçbir şey yapma, sıradakine geç.
                continue;
            }
            // Yeni durumu yaz.
            states.put(effect.object(), effect.state());
            // Bu duruma şimdi girdiğini kaydet.
            enteredAt.put(effect.object(), second);
            // Olayı listeye ekle.
            events.add(new TimelineEvent(second, effect.object(), current, effect.state(), ruleId, caption));
        }
    }
}
