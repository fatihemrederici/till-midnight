# JSON dosyalarının açıklaması

JSON dosyalarına yorum satırı yazılamaz, bu yüzden açıklamaları burada.

## `scenes/cafe-candle.json`

### Üst kısım

| Alan | Anlamı |
| --- | --- |
| `formatVersion` | Format sürümü. Şimdilik 1. |
| `id` | Sahnenin benzersiz adı. |
| `title` | Oyuncunun göreceği başlık. |
| `intro` | Oyun başındaki kısa giriş cümlesi. |
| `durationSeconds` | Sahnenin süresi: 10 saniye. |
| `maxTouches` | Biriktirilebilecek en fazla dokunuş: 2. |
| `grid` | Izgaranın sütun ve satır sayısı. |

### `objects`: nesneler

| Alan | Anlamı |
| --- | --- |
| `id` | Kodun nesneyi tanıdığı ad. |
| `label` | Oyuncunun göreceği ad. |
| `position` | Izgaradaki yeri. Sayım 0'dan başlar. |
| `initialState` | 0. saniyedeki durumu. |
| `states` | Olabileceği tüm durumlar ve her birinin emojisi. |

Her nesne her an tek bir durumdadır.

### `touches`: oyuncu dokununca ne olur

| Alan | Anlamı |
| --- | --- |
| `id` | Kuralın adı. |
| `object` | Dokunulan nesne. |
| `when` | Kuralın çalışması için gereken koşullar. Hepsi doğru olmalı. |
| `then` | Kural çalışınca değişecek durumlar. |
| `caption` | Ekranda gösterilecek cümle. |

Aynı nesne için ilk uyan kural çalışır. Hiçbiri uymazsa dokunuşun etkisi olmaz.

### `triggers`: kendiliğinden olanlar

Her tetikleyicide `at` ya da `after` bulunur, ikisi birden olmaz.

| Alan | Anlamı |
| --- | --- |
| `id` | Kuralın adı. |
| `at` | Çalışacağı saniye. |
| `after` | Nesne o duruma girdikten `delay` saniye sonra çalışır. Nesne o durumdan çıktıysa çalışmaz. |
| `when` | Çalışma anında doğru olması gereken koşullar. İsteğe bağlı. |
| `then` | Değişecek durumlar. |
| `caption` | Ekranda gösterilecek cümle. İsteğe bağlı. |

### `disasters`: kaybetme koşulları

| Alan | Anlamı |
| --- | --- |
| `id` | Felaketin adı. |
| `when` | Bu koşullar oluşursa döngü biter. |
| `caption` | Ekranda gösterilecek cümle. |

### Kafe sahnesinin hikâyesi

İki zincir var:

- **Makine:** 1. saniyede ısınır, 3. saniyede basınç yükselir, 8. saniyede patlar. Basınç varken vanayı açmak zinciri kırar.
- **Yangın:** 2. saniyede kedi uyanır, 4. saniyede mumu devirir, 6. saniyede perde dumanlanır, 9. saniyede yanar.

Dokunmazsan makine önce patladığı için yangını göremezsin. Yangın ancak makineyi çözünce ortaya çıkar.

En şık çözüm 3. saniyede vanayı açmak: buhar 4. saniyede mumu söndürür ve tek dokunuş iki zinciri de kırar.

## `scenes/scene.schema.json`

Sahne dosyalarının uyması gereken yapıyı anlatır. AI'ın ürettiği sahneleri denetlemek için kullanılacak.

| Alan | Anlamı |
| --- | --- |
| `$schema`, `$id` | JSON Schema sürümü ve bu şemanın adı. |
| `type: object` | Sahne bir JSON nesnesi olmalı. |
| `additionalProperties: false` | Listede olmayan alan yazılamaz. |
| `required` | Zorunlu alanlar. |
| `properties` | Her alanın tipi ve sınırları, örneğin `maxTouches` 1 ile 3 arasında. |
| `$defs` | Tekrar kullanılan parçalar: `sceneObject`, `touchRule`, `trigger`, `disaster`. |
| `$ref` | `$defs` içindeki bir parçayı işaret eder. |
| `pattern` | `id` alanlarının yazım kuralı: küçük harf ve rakam, araları `-` ya da `_`. |
| `oneOf` | Tetikleyicide `at` ya da `after` alanlarından tam olarak biri olmalı. |

Şema yapıyı kontrol eder, anlamı kontrol edemez. Örneğin var olmayan bir nesneye başvurulmasını yakalayamaz; bunu ileride sunucu tarafındaki doğrulama yapacak.
