# Backend Standartları — E-Commerce Marketplace Clone

> **Versiyon:** 1.0
> **Kaynak:** `PROJECT_PLAN.md` ve `DATABASE_SCHEMA.md` (bu dizinde)
> **Durum:** Aktif

Bu doküman, bu projenin backend'i için mimari kararları ve kodlama standartlarını tek doğruluk kaynağı olarak tanımlar. `PROJECT_PLAN.md`'de zaten karara bağlanmış konuların (auth mimarisi, veri modeli, roadmap) özetlenmiş halidir — çelişki durumunda `PROJECT_PLAN.md` esastır.

---

## İçindekiler

1. [Mimari](#1-mimari)
2. [Transaction Yönetimi](#2-transaction-yönetimi)
3. [Security & Authentication](#3-security--authentication)
4. [Kodlama Standartları](#4-kodlama-standartları)
5. [API & Controller Standartları](#5-api--controller-standartları)
6. [Exception Handling](#6-exception-handling)
7. [Logging](#7-logging)
8. [JPA & Veri Standartları](#8-jpa--veri-standartları)
9. [Test Standartları](#9-test-standartları)
10. [Git & Commit Standartları](#10-git--commit-standartları)
11. [AI Kullanımı](#11-ai-kullanımı)
12. [Anti-Pattern Referans Tablosu](#12-anti-pattern-referans-tablosu)
13. [Değiştirilemez Kurallar](#13-değiştirilemez-kurallar-özet)
14. [Tech Stack](#14-tech-stack)
15. [Kullanılmayan Ağır Yapılar](#15-kullanılmayan-ağır-yapılar)

---

## 1. Mimari

- **Katmanlı mimari:** Controller → Service → Repository → DB. Modüler monolith değil — tek modül, basit paket yapısı (staj projesindeki gibi).
- Entity hiçbir zaman Controller sınırının dışına (request/response'a) sızmaz; yalnızca DTO'lar taşınır.
- Entity ↔ DTO dönüşümü manuel Mapper sınıflarıyla yapılır (`ProductMapper`, `OrderMapper` deseni) — MapStruct kullanılmaz, gereksiz bir bağımlılık.
- Katmanlar arası bağımlılık tek yönlü: Controller → Service → Repository. Controller'a iş mantığı yazılmaz.

## 2. Transaction Yönetimi

- Transaction sınırı Service katmanında başlar (`@Transactional`); Controller'da asla.
- Salt okuma metodları `@Transactional(readOnly = true)`.
- Çok adımlı iş akışları (checkout, satıcı onayı, adres varsayılanı değiştirme) **tek transaction** içinde yürütülür — kısmi başarı durumu olmaz, herhangi bir adım başarısız olursa hepsi geri alınır.

## 3. Security & Authentication

- **Access token:** Kısa ömürlü (20 dk), yalnızca `Authorization: Bearer` header ile taşınır, JSON gövdesinde döner — cookie'ye asla konmaz.
- **Refresh token:** httpOnly, Secure, `SameSite=None` cookie + DB'de (`refresh_tokens`) iz sürülür; her `/refresh` çağrısında rotate edilir (eskisi geçersiz, yenisi verilir).
- Şifreler BCrypt ile hash'lenir; düz metin şifre asla DB'ye yazılmaz veya loglanmaz.
- Yetkilendirme her zaman deklaratif (`@PreAuthorize("hasRole('...')")`); Controller/Service içinde manuel `if (user == null)` veya rol string karşılaştırması yasak.
- Sahiplik kontrolü (bir kaynağın gerçekten o kullanıcıya/satıcıya ait olduğu) sorgu seviyesinde yapılır (`findByIdAndSellerId` gibi), asla "çek, sonra kod içinde karşılaştır" değil.
- CORS: tek, açık origin (`*` yasak) + `Access-Control-Allow-Credentials: true`.
- Secrets (`JWT_SECRET`, `RESEND_API_KEY` vb.) yalnızca environment variable; kodda veya `application.yaml`'da default değer yok.
- Admin self-serve bir endpoint ile oluşturulmaz — yalnızca elle DB güncellemesi.
- Rate limiting hassas endpoint'lerde zorunlu (`login`, `register`, `checkout`, `forgot-password`).
- Yerel geliştirmede `Secure` bayrağı bir Spring profile ile kapatılabilir (`app.cookie.secure`), prod'da her zaman `true`.

## 4. Kodlama Standartları

- **Injection:** Constructor injection zorunlu (`@RequiredArgsConstructor`). Field injection (`@Autowired`) yasak.
- **Entity:** `@Getter`/`@Setter`/`@Builder` kullanılır; `@Data` yasak (equals/hashCode ve mutability sorunları).
- **DTO:** Java `record` — immutable, boilerplate'siz.
- **Validasyon:** Format kontrolleri (`@NotBlank`, `@Email`, `@Positive` vb.) DTO üzerinde + `@Valid`; iş kuralları Service katmanında.

## 5. API & Controller Standartları

### 5.1 Varsayılan: POJO + `@ResponseStatus`

Controller metodları varsayılan olarak DTO döner, HTTP status'u `@ResponseStatus` ile deklaratif belirtilir. Bu, metodun ne döndüğünü belirsizleştirmez ve Swagger/OpenAPI'nin dönüş tipini doğru analiz etmesini sağlar.

### 5.2 İstisna: `ResponseEntity`

Yalnızca **header manipülasyonu gerektiğinde** kullanılır — bu projede en somut örneği login/refresh/logout endpoint'lerinin `Set-Cookie` header'ı ile refresh token döndürmesidir. Header ihtiyacı yoksa `ResponseEntity` sarmalayıcısı kullanılmaz.

### 5.3 Diğer Kurallar

- Entity asla Controller'a girmez/çıkmaz — yalnızca DTO.
- Listeleme endpoint'leri `Pageable` kullanır; manuel `page`/`size` parametresi yasak.
- Public endpoint'ler `@Operation`/`@ApiResponse` ile dokümante edilir (springdoc-openapi); auth şeması global tanımlanır, endpoint başına tekrarlanmaz.
- `ResponseEntity<?>` veya `ResponseEntity<Object>` (tiplendirilmemiş) yasak.

## 6. Exception Handling

- Tek bir `@RestControllerAdvice` (`GlobalExceptionHandler`); istemciye asla stacktrace dönmez.
- Tutarlı hata formatı: `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`, opsiyonel `fieldErrors`) — staj projesindeki format, RFC 7807 değil.
- Her iş kuralı ihlali kendi exception sınıfına sahiptir (`ResourceNotFoundException`, `DuplicateResourceException`, `InsufficientStockException`, `EmptyCartException`, `InvalidOrderStatusTransitionException`, `InvalidRefreshTokenException`, `SellerApplicationException` vb.) — generic `RuntimeException` fırlatılmaz.
- `ObjectOptimisticLockingFailureException` (Spring'in `@Version` çakışmasında fırlattığı) özel olarak yakalanıp temiz bir 409'a çevrilir — çıplak 500 olarak sızmaz.

## 7. Logging

- Yapılandırılmış, tutarlı formatlı loglama; her istek için method/path/status/süre.
- Şifre, JWT token, email doğrulama/reset token'ı gibi hassas veriler **asla** loglanmaz.
- `e.printStackTrace()` yasak — her zaman `log.error("...", e)`.

## 8. JPA & Veri Standartları

- Şema yönetimi Flyway ile versiyonlanır; `ddl-auto` her zaman `validate`, asla `update` değil.
- Enum alanları `@Enumerated(EnumType.STRING)` — ordinal mapping yasak.
- Silme varsayılan olarak **soft delete**'tir (`products.is_active` gibi) — geçmiş kayıtlarla (`order_items`) FK bütünlüğü korunur.
- Eşzamanlılık riski olan alanlarda (`products.stock_quantity`) optimistic locking (`@Version`).
- Para ve adres gibi zamana bağlı veriler işlem anında **snapshot**'lanır, kaynağa FK verilmez (`order_items.unit_price`, `orders`'daki adres alanları).
- Tek bir varsayılan (`is_default`) gibi kurallar DB kısıtı değil, servis katmanında garanti edilir.

## 9. Test Standartları

- **Unit testler** (`*Test.java`, Surefire): Service katmanı, Repository `@Mock` ile mocklanır, assertion AssertJ ile.
- **Integration testler** (`*IT.java`, Failsafe): `@SpringBootTest` + MockMvc + Testcontainers (gerçek PostgreSQL) — H2 kullanılmaz.
- Her PR'da CI ikisini de çalıştırır; testler geçmeden `main`'e merge edilmez.
- Yalnızca happy path değil, hata senaryoları da test edilir (yetkisiz erişim, sahiplik izolasyonu, stok/optimistic-lock çakışması vb.).
- Test yazımı ayrı bir "test haftası" değil, her haftanın "definition of done"ının bir parçasıdır.

## 10. Git & Commit Standartları

- **Conventional Commits:** `feat:`, `fix:`, `docs:`, `test:`, `chore:`.
- `main`'den `feature/<isim>` branch'i açılır, PR ile merge edilir (merge commit, squash değil), feature branch merge sonrası silinir.
- `main` her zaman deploy edilebilir durumda kalır; doğrudan üzerine commit atılmaz.
- CI (`mvn verify`) yeşil olmadan PR merge edilmez.

## 11. AI Kullanımı

Bu proje Claude ile birlikte geliştiriliyor. Backend kodu yazılırken bu dokümana ve `PROJECT_PLAN.md`'ye (özellikle Security, Exception Handling, Transaction Yönetimi bölümlerine) uyulması beklenir.

## 12. Anti-Pattern Referans Tablosu

| ❌ Yanlış | ✅ Doğru |
|---|---|
| Field injection (`@Autowired`) | Constructor injection |
| Controller'da `if (user == null)` | `@PreAuthorize` |
| Entity döndürmek/almak | DTO döndürmek/almak |
| `findById().get()` | `findById().orElseThrow()` |
| Manuel `page`/`size` parametresi | `Pageable` |
| `ddl-auto=update` | Flyway + `ddl-auto=validate` |
| Refresh token'ı JSON body'de dönmek | httpOnly Secure cookie |
| Access token'ı localStorage'da beklemek | Yalnızca header + FE memory |
| Entity'de `@Data` | `@Getter`/`@Setter`/`@Builder` |
| `e.printStackTrace()` | `log.error("...", e)` |
| Genel `RuntimeException` fırlatmak | Domain'e özel exception sınıfı |
| H2 ile test | Testcontainers + gerçek PostgreSQL |
| Stacktrace istemciye dönmek | `GlobalExceptionHandler` + `ErrorResponse` |
| Soft-delete edilmiş kaydı hard-delete etmek | `is_active=false` |
| `@Version` çakışmasını yakalamamak | Temiz 409'a çevirip dönmek |
| Sahipliği koda çekip karşılaştırmak | Sorguya gömmek (`findByIdAndSellerId`) |
| `ResponseEntity<?>` / `ResponseEntity<Object>` | Tiplendirilmiş POJO + `@ResponseStatus` |

## 13. Değiştirilemez Kurallar (Özet)

| Kural | Durum |
|---|---|
| Field injection | ❌ Yasak |
| Controller'da manuel `null`/rol kontrolü | ❌ Yasak |
| Access veya refresh token localStorage'da | ❌ Yasak |
| Enum ordinal mapping | ❌ Yasak |
| Manuel pagination | ❌ Yasak |
| Secret'lar kodda/yml'de/Git'te | ❌ Yasak |
| `ddl-auto=update` | ❌ Yasak |
| H2 (test dahil) | ❌ Yasak |
| Entity'de `@Data` | ❌ Yasak |
| Entity'nin Controller'a sızması | ❌ Yasak |
| Stacktrace istemciye dönmek | ❌ Yasak |
| `findById().get()` | ❌ Yasak |
| `e.printStackTrace()` | ❌ Yasak |
| Ürünü hard-delete etmek | ❌ Yasak |
| Admin'in self-serve oluşturulması | ❌ Yasak |

## 14. Tech Stack

*(Şu an bu projede kullandığımız teknolojiler — alfabetik sıra)*

AssertJ · BCrypt · bucket4j · Conventional Commits · Failsafe (Maven Plugin) · Flyway · GitHub Actions · Java 21 · jjwt · JUnit 5 · Lombok · Maven · Mockito · PostgreSQL · Railway · Resend · Spring Boot · Spring Data JPA · Spring Security · springdoc-openapi (Swagger UI) · Surefire (Maven Plugin) · Testcontainers

## 15. Kullanılmayan Ağır Yapılar

*(Bu projede bilinçli olarak kullanılmayan/tercih edilmeyen ağır yapılar — alfabetik sıra. Yeni bir şey eklemeden önce burada olup olmadığına bak.)*

API Gateway · CQRS · Distributed Tracing (Zipkin/Jaeger) · Docker Swarm / Kubernetes (Orkestrasyon) · Elasticsearch · Event Sourcing · GraphQL · gRPC · Kafka / RabbitMQ (Message Queue) · Microservices · MongoDB / NoSQL Veritabanları · Multi-tenancy · Redis (Cache) · Saga Pattern · Service Mesh · Sharding · Staging / Çoklu Ortam Pipeline'ı · WebSocket / Real-time İletişim

---

> **Doküman Sonu** — Sorular için `PROJECT_PLAN.md`'ye bakın ya da Claude ile konuşun.
