# E-Commerce Marketplace Clone — Proje Planı

**Tür:** Kişisel portföy projesi (staj projesi `ecommerce-simulation-api`'den ayrı, onun üzerine kurulan bilgiyle geliştirilir)
**Doküman Versiyonu:** 1.0
**Son güncelleme:** bu dokümanı oluşturduğumuz konuşma

---

## 1. Amaç ve Kapsam

Gerçek bir e-ticaret **marketplace**'inin (Trendyol/Etsy benzeri — tek mağaza değil, birden fazla satıcı) sade ama modern standartlarda bir klonu. Hedef: haftada bir dilim tamamlanıp `main`'e mergelendiğinde otomatik olarak canlıya çıkması, portföyde gösterilebilecek, gerçekten kullanılabilir bir ürün.

**Bilinçli olarak kapsam dışı bırakılanlar:** microservice mimarisi, gerçek ödeme entegrasyonu (Stripe vb. — "Satın Al" doğrudan sipariş oluşturur), aynı ürünün birden fazla satıcı tarafından satılması (her ürün tek bir satıcıya ait — "buy box" problemi yok).

## 2. Rol Modeli

| Rol | Nasıl kazanılır | Yetkileri |
|---|---|---|
| USER | Kayıt olan herkes buradan başlar | Ürün görüntüleme, sepet, sipariş verme, satıcı olmaya başvurma |
| SELLER | USER olarak başvurur, ADMIN onaylar | Kendi ürünlerini yönetir, kendine gelen siparişleri yönetir |
| ADMIN | Self-serve YOK — normal kayıt + veritabanında elle `role='ADMIN'` | Kategori yönetimi, satıcı başvurularını onaylama/reddetme, platform geneli denetim, her siparişi override edebilme |

Bir kullanıcı aynı anda yalnızca bir rolde olabilir (`User.role` tek alan).

## 3. Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Backend | Java 21, Spring Boot 4.x, PostgreSQL, Spring Data JPA, Spring Security + JWT |
| Migration | Flyway (`ddl-auto: validate`, asla `update` değil) |
| Frontend | React + Vite + TypeScript + Tailwind |
| Email | Resend (email doğrulama + şifre sıfırlama linkleri için) |
| Rate limiting | bucket4j (in-memory, tek instance için yeterli) |
| Backend hosting | Railway (~5-10$/ay, Postgres dahil) |
| Frontend hosting | Vercel (ücretsiz katman) |
| CI/CD | GitHub Actions — backend'de `mvn verify`, frontend'de `npm run build` (+ lint), ikisi de PR'da; platformların kendi git-push-to-deploy entegrasyonu |
| Ortam | Tek ortam: `main` → doğrudan prod, staging yok |

**Repo yapısı:** İki ayrı repo — `ecommerce-clone-api` (backend) ve `ecommerce-clone-web` (frontend). Her biri kendi platformuna bağımsız deploy olur.

**Sorumluluk paylaşımı:** Backend'i İhsan yazar (kendi öğrenme/kariyer odağı). Frontend'i bir AI kod asistanı (ayrı bir terminal/oturumda) yazar ve sürdürür.

### İşbirliği Süreci (BE ↔ FE Senkronizasyonu)

Backend ve frontend farklı repo'larda, farklı AI oturumlarında (terminallerde) geliştirilir. Bu iki oturumun canlı/otomatik olarak birbirine bağlanmasına **gerek yok ve önerilmez** — bu, projeye değer katmayan gereksiz bir koordinasyon katmanı olurdu. Bunun yerine:

1. **Sıra her zaman BE → FE'dir, paralel değil.** Bir haftanın backend dilimi bitirilir, `main`'e merge edilir, Railway'e deploy olur — **sonra** o haftanın frontend dilimine başlanır.
2. **FE her zaman gerçek, deploy edilmiş API'ye göre yazılır**, bu dokümandaki plana göre değil. Kod yazarken plandan küçük sapmalar olması normaldir (bir endpoint'in response şekli değişebilir, bir alan eklenebilir).
3. **Bu doküman, iki oturum arasındaki paylaşılan sözleşmedir.** Backend'de plandan sapan bir karar alınırsa, FE'ye geçmeden önce bu sapma `PROJECT_PLAN.md` ve/veya `DATABASE_SCHEMA.md`'ye yazılır. FE oturumu işe başlarken güncel dokümana + gerçek API'ye bakar.
4. Pratik akış: BE terminalinde hafta biter → PR merge → deploy → (varsa) dokümanlar güncellenir → FE terminali açılır, o haftanın görevleri + güncel dokümanlar referans verilerek işe başlanır.

### Git & Deploy Workflow

Staj projesindeki (`ecommerce-simulation-api`) git geçmişinden aynen taşınan desen:

**Repo adları (GitHub, `ihsanerben/` altında):**
- `github.com/ihsanerben/ecommerce-clone-api` — backend, Railway'e bağlı
- `github.com/ihsanerben/ecommerce-clone-web` — frontend, Vercel'e bağlı

**Branch stratejisi:**
- `main` her zaman deploy edilebilir durumda kalır, doğrudan üzerine commit atılmaz.
- Her özellik/hafta için `main`'den bir `feature/<özellik-adı>` branch'i açılır. Roadmap'teki (§6) haftalarla uyumlu örnek isimler: `feature/project-setup`, `feature/auth-refresh-token`, `feature/password-reset`, `feature/seller-onboarding`, `feature/product-catalog`, `feature/category-search-address`, `feature/cart`, `feature/checkout`, `feature/order-management`.
- Bir haftanın kapsamı büyükse (örn. çok endpoint içeriyorsa) o hafta içinde birden fazla daha küçük `feature/` branch'i de açılabilir — branch'i haftaya değil, tamamlanan tek bir mantıksal işe göre kapatmak daha sağlıklıdır.
- Commit mesajları **Conventional Commits** formatında: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, `chore: ...` (staj projesindeki commit geçmişiyle birebir aynı stil).

**PR ve merge akışı:**
1. Özellik branch'inde çalışılır, commit'lenir, GitHub'a push edilir.
2. `feature/xxx` → `main` bir Pull Request açılır.
3. GitHub Actions CI PR üzerinde otomatik çalışır — backend repo'sunda `mvn verify`, frontend repo'sunda `npm run build` (+ lint); kırmızıysa merge edilmez.
4. CI yeşil olunca PR merge edilir (staj projesindeki gibi "Merge pull request #N" — merge commit, squash değil), feature branch silinir.

### Ortam Değişkenleri (Env Vars)

**Backend (Railway'de tanımlanır):**
| Değişken | Açıklama |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Railway managed Postgres bağlantısı |
| `JWT_SECRET` | Access token imzalama anahtarı — staj projesindeki gibi zorunlu, kodda default yok |
| `JWT_ACCESS_EXPIRATION_MS` | Access token ömrü (örn. `1200000` = 20 dk) |
| `JWT_REFRESH_EXPIRATION_DAYS` | Refresh token ömrü (örn. `14`) |
| `PASSWORD_RESET_EXPIRATION_MINUTES` | Şifre sıfırlama token ömrü (varsayılan `60`) |
| `CORS_ALLOWED_ORIGIN` | Frontend'in Vercel URL'i (yerelde `http://localhost:5173`) |
| `COOKIE_SECURE` | Refresh token cookie'sinin `Secure` bayrağı — yerelde `false`, Railway'de `true` (varsayılan) |
| `RESEND_API_KEY` | Doğrulama/şifre sıfırlama emaili göndermek için |
| `RESEND_FROM_EMAIL` | Resend üzerinde doğrulanmış gönderici adresi |
| `FRONTEND_BASE_URL` | Şifre sıfırlama linkinin işaret edeceği adres (örn. `https://app.domain.com/reset-password`) |

**Frontend (Vercel'de tanımlanır):**
| Değişken | Açıklama |
|---|---|
| `VITE_API_BASE_URL` | Backend'in Railway URL'i |

**Deploy sıklığı:** Ayrı bir deploy takvimi yoktur — Railway/Vercel, `main`'e her merge'de otomatik deploy tetikler (continuous deployment). Yani "ne sıklıkla deploy edilecek" sorusunun cevabı "ne sıklıkla `main`'e merge edilirse" — pratikte bu genelde haftalık bir dilimin PR'ı merge olduğunda gerçekleşir, ama hafta içinde küçük bir düzeltme için ayrı bir merge de kendi deploy'unu tetikler. Bilerek staging/onay adımı olmayan bir akış (§3 "Ortam" satırı) — merge = canlıda.

## 4. Mimari Kararlar ve Gerekçeleri

### 4.1 Auth ve Oturum

- **Access token:** Kısa ömürlü (öneri: **20 dk**, `JWT_ACCESS_EXPIRATION_MS`), login/refresh response'unun **JSON gövdesinde** döner, frontend'de yalnızca memory'de tutulur, `Authorization: Bearer` header ile API isteklerine eklenir. localStorage kullanılmaz (XSS'e karşı).
- **Refresh token — httpOnly cookie (sektör standardı):** Access token'ın aksine JSON gövdesinde hiç dönmez. Production'da backend `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=None; Path=/api/auth` header'ını kullanır; localhost'ta ise tarayıcı uyumluluğu için `Secure=false; SameSite=Lax` uygular. JS bu cookie'yi hiç okuyamaz (XSS'e karşı en güçlü koruma), tarayıcı otomatik gönderir. DB'de de (`refresh_tokens` tablosu) ayrıca saklanır — uzun ömürlü (öneri: **14 gün**, `JWT_REFRESH_EXPIRATION_DAYS`), rotation ve revoke için.
  - **Cross-site detayı:** Backend (Railway) ve frontend (Vercel) farklı domain'lerde olduğu için `SameSite=None` zorunlu (aksi halde tarayıcı cookie'yi cross-site isteklerde göndermez). Bunun çalışması için: (1) `Secure` şart (yalnızca HTTPS — Railway/Vercel zaten HTTPS), (2) backend CORS config'i `Access-Control-Allow-Credentials: true` döndürmeli ve origin'i `*` değil **tam olarak** frontend'in Vercel URL'i olmalı (zaten `CORS_ALLOWED_ORIGIN` ile böyle), (3) frontend'deki her `fetch`/axios isteği `credentials: 'include'` ile gitmeli. Custom domain eklenip FE/BE aynı kök domain'in alt domain'i olduğunda (backlog, §8) `SameSite=None` yerine daha sıkı `SameSite=Lax`'e geçilebilir — zorunlu değil, iyileştirme.
  - **Yerel geliştirme:** `COOKIE_SECURE=false` olduğunda cookie `Secure=false, SameSite=Lax`; Railway'de `COOKIE_SECURE=true` olduğunda `Secure=true, SameSite=None` olur. Böylece localhost HTTP üzerinde `SameSite=None` cookie'nin tarayıcı tarafından reddedilmesi önlenir.
- **Rotation:** Her `/api/auth/refresh` çağrısında DB'deki eski refresh token satırı geçersiz kılınır, yenisi üretilip hem DB'ye yazılır hem yeni cookie olarak set edilir — çalınan bir refresh token'ın tekrar tekrar kullanılmasını (replay) önler.
- **Gerçek logout:** `/api/auth/logout` hem DB'deki refresh token'ı `revoked=true` yapar hem cookie'yi `Max-Age=0` ile temizler.
- **Yan fayda:** Access token kısa ömürlü olduğu için rol değişiklikleri (örn. satıcı onayı) en geç bir sonraki refresh'te (~20 dk içinde) otomatik yansır, kullanıcı elle tekrar login olmak zorunda kalmaz. Sayfa yenilendiğinde (F5) access token memory'den silinir ama refresh cookie'si tarayıcıda kalır — FE açılışta sessizce `/api/auth/refresh` çağırıp yeni bir access token alır, kullanıcı oturumda kalmaya devam eder.
- **Email doğrulama:** Kayıt anında `users.email_verified=false` ile başlanır, doğrulama linki mail atılır (bkz. Hafta 2). Doğrulanmamış hesap login olamaz. Resend entegrasyonu bu yüzden Hafta 2'ye çekildi (aşağıda).
- **Şifre sıfırlama:** `password_reset_tokens` tablosu (refresh token ile aynı desende — token + expiry + used). Hafta 2'de zaten kurulmuş olan Resend altyapısı yeniden kullanılır. `forgot-password` endpoint'i, email sistemde olsun olmasın her zaman 200 döner (kullanıcı numarası taramasını/enumeration'ı önlemek için).

### 4.2 Satıcı Onboarding

1. USER → `POST /api/sellers/apply` (mağaza adı + açıklama) → `Seller.status = PENDING`
2. ADMIN → bekleyen başvuruları görür, onaylar/reddeder
3. Onayda **tek transaction içinde** hem `Seller.status = APPROVED` hem `User.role = SELLER` güncellenir (ikisi ayrı ayrı güncellenirse tutarsız bir ara durum oluşabilir)
4. Reddedilen kullanıcı aynı `Seller` satırını güncelleyerek tekrar başvurabilir (ayrı bir başvuru geçmişi tutulmaz)

### 4.3 Ürün ve Stok

- Her `Product` tam olarak bir `Seller`'a ait (`seller_id`), asla client'tan alınmaz — backend, giriş yapan satıcının kendi `Seller` kaydını `user_id` üzerinden bulup otomatik atar.
- **Soft delete:** Ürün "silme" aslında `is_active = false` yapar. Sebep: geçmiş siparişlerdeki `OrderItem` satırları hâlâ o ürüne referans veriyor — hard delete bu kayıtları kırar. Public listeleme yalnızca `is_active = true` ürünleri gösterir.
- **Optimistic locking:** `Product.version` (`@Version`) — eşzamanlı iki checkout'un aynı ürünün stoğunu birbirini ezerek düşürmesini (overselling) engeller.
- **Kategori:** Yalnızca ADMIN oluşturur/düzenler/siler. Satıcılar var olan kategoriden seçer, yeni kategori icat edemez (taksonomi kirlenmesini önlemek için).
- **Görsel:** Yalnızca `image_url` (string) alanı — satıcı dışarıdan bir görsel linki yapıştırır. Gerçek dosya yükleme servisi (Cloudinary/R2) backlog'da.

### 4.4 Sepet ve Checkout

- Sepet birden fazla satıcıdan ürün içerebilir, herhangi bir kısıtlama yok — ayrım checkout anında yapılır.
- **Checkout = satıcı başına ayrı sipariş:** Sepet satıcıya göre gruplanır; her grup için ayrı bir `Order` oluşturulur (çünkü her satıcı kendi siparişini bağımsız yönetir/kargolar). Tek "Satın Al" tıklaması N satıcı varsa N sipariş üretebilir.
- Süreç: teslimat adresi seçimi zorunlu → her satır için hem stok kontrolü hem **`is_active=true` kontrolü** (fail-fast, hepsi kontrol edilmeden hiçbir mutasyon yapılmaz — bir ürün sepete eklendikten sonra satıcı tarafından soft-delete edilmiş olabilir, checkout bunu yakalamalı) → **tek transaction** içinde satıcı başına `Order`+`OrderItem` oluştur → sepeti boşalt. Herhangi bir adım başarısız olursa hepsi geri alınır.
- **Adres snapshot:** `Order`, teslimat bilgisini `Address` tablosuna FK ile değil, **kendi satırında snapshot alanlar olarak** tutar (`recipient_name`, `phone`, `address_line`, `city`, `postal_code`). Aynı `OrderItem.unit_price`'ın fiyatı sipariş anında dondurması gibi — kullanıcı adresini sonradan düzenlese/silse bile geçmiş siparişler bozulmaz.
- `Address` ayrı bir entity + tam CRUD'a sahip — kullanıcı birden fazla adres kaydedip checkout'ta seçebilir.
- **Tek varsayılan adres kuralı:** `is_default` DB seviyesinde değil (Postgres'te "yalnızca bir satır true olabilir" kısıtı partial unique index gerektirir, burada gerek yok), **servis katmanında** garanti edilir — bir adres varsayılan yapılırken aynı kullanıcının diğer tüm adresleri tek transaction içinde `is_default=false` yapılır.

### 4.5 Sipariş Durum Makinesi

```
PENDING → CONFIRMED → SHIPPED → DELIVERED
   ↓           ↓
   └──→ CANCELLED ←──┘
```

(Tüm satıcı-tetiklemeli geçişler — PENDING→CONFIRMED dahil — aynı `PUT /api/seller/orders/{id}/status` endpoint'inden yapılır, ayrı bir "onay" endpoint'i yoktur.)

- **Satıcı**, kendisine gelen yeni siparişleri satıcı panelinden görüp onaylar (**PENDING → CONFIRMED** de dahil olmak üzere tüm normal akışı satıcı ilerletir: PENDING → CONFIRMED → SHIPPED → DELIVERED). Yani checkout bir siparişi doğrudan CONFIRMED yapmaz — satıcının onu görüp kabul ettiğini garanti eder.
- **Alıcı** yalnızca PENDING/CONFIRMED aşamasındayken iptal edebilir; iptalde stok iade edilir.
- **Admin** her zaman, her durumu, her siparişte override edebilir (anlaşmazlık çözümü için).
- `PAID` durumu yok — gerçek ödeme akışı olmadığı için anlamsız.

### 4.6 Diğer Kesitler

- **CORS:** Backend yalnızca frontend'in Vercel origin'ine izin verir (env var ile yönetilir, tam origin — `*` değil), `Access-Control-Allow-Credentials: true` ile — refresh token cookie'sinin cross-site gönderilebilmesi için zorunlu (bkz. §4.1).
- **Rate limiting:** `login`, `register`, `checkout`, `forgot-password` gibi hassas endpoint'lerde bucket4j ile.
- **Logging:** Yapılandırılmış (tutarlı formatlı), her istek için method/path/status/süre loglanır.
- **Admin oluşturma:** Kod yok — normal `/api/auth/register` ile hesap açılır, Railway Postgres konsolundan elle `UPDATE users SET role = 'ADMIN' WHERE username = '...'` çalıştırılır.
- **Hata yönetimi:** Staj projesindeki `GlobalExceptionHandler` + `ErrorResponse` (timestamp/status/error/message/path) deseni aynen taşınır. Yeni domain'in getirdiği ek exception'lar: `InvalidRefreshTokenException` (401), `ExpiredRefreshTokenException` (401), `InvalidResetTokenException` (400), `SellerApplicationException` (örn. onaylı satıcı tekrar başvuramaz — 409), `InvalidOrderStatusTransitionException` (409). Ayrıca `@Version` çakışmasında Spring'in fırlattığı `ObjectOptimisticLockingFailureException` özel olarak yakalanıp temiz bir 409 ("bu ürün bu sırada başka biri tarafından güncellendi, tekrar deneyin") olarak dönmeli — yoksa çıplak bir 500 olarak sızar.
- **API doğrulama & dokümantasyon:** Staj projesindeki Bean Validation (`@NotBlank`, `@Email`, `@Positive` vb. + `@Valid`) ve springdoc-openapi (Swagger UI, `@Operation`/`@ApiResponse`) alışkanlığı bu projede de devam eder.

### 4.7 Frontend Mimarisi

Admin paneli ve satıcı paneli **ayrı uygulamalar değildir** — tek bir React app içinde role'e göre kapılı rota grupları:
- `/admin/*` — yalnızca `role === ADMIN` görebilir (kategori yönetimi, satıcı onayı, genel denetim)
- `/seller/*` — yalnızca `role === SELLER` görebilir (kendi ürünleri, kendine gelen siparişler)
- Geri kalan her şey (ürün gezme, sepet, checkout, siparişlerim) normal alıcı deneyimi

FE'deki rota koruması yalnızca kullanıcı deneyimi içindir, **gerçek güvenlik backend'deki `@PreAuthorize` kontrolleridir** — FE her zaman API'nin kendi yetki kontrolüne güvenir, kendi başına yetki vermez.

Bir kullanıcının satıcı-ilişkili ekranlarda görebileceği 4 durum vardır, FE routing bu duruma göre dallanır:

| Durum | Kullanıcı ne görür |
|---|---|
| Hiç başvurmamış USER | "Satıcı Ol" çağrısı/butonu |
| Başvurmuş, PENDING | "Başvurunuz inceleniyor" sayfası |
| REJECTED | Red mesajı + tekrar başvur seçeneği |
| APPROVED (role artık SELLER) | Tam satıcı paneli (`/seller/*`) |

## 5. Endpoint Referansı

### Auth
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/auth/register` | Herkes | Kayıt, rol=USER ile başlar |
| POST | `/api/auth/login` | Herkes | Giriş, access+refresh token döner |
| POST | `/api/auth/refresh` | Geçerli refresh token (httpOnly cookie) | Yeni access token JSON'da döner, refresh cookie rotate edilir |
| POST | `/api/auth/logout` | Giriş yapmış | Refresh token'ı DB'de iptal eder + cookie'yi temizler |
| POST | `/api/auth/verify-email` | Geçerli doğrulama token | `email_verified=true` yapar |
| POST | `/api/auth/resend-verification` | Doğrulanmamış hesap sahibi | Eski token'ı geçersiz kılıp yeni doğrulama maili gönderir (link süresi dolmuş/kaybolmuşsa çıkış yolu) |
| POST | `/api/auth/forgot-password` | Herkes | Reset linkini email ile gönderir |
| POST | `/api/auth/reset-password` | Geçerli reset token | Yeni şifreyi kaydeder |

### Satıcı Onboarding
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/sellers/apply` | USER | Satıcı başvurusu oluşturur/günceller |
| GET | `/api/sellers/me` | USER/SELLER | Kendi başvuru durumunu görür |
| GET | `/api/admin/sellers?status=PENDING` | ADMIN | Bekleyen başvuruları listeler |
| PUT | `/api/admin/sellers/{id}/approve` | ADMIN | Onaylar (Seller+User rolü birlikte) |
| PUT | `/api/admin/sellers/{id}/reject` | ADMIN | Reddeder |

### Kategori
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/categories` | Herkes | Liste |
| GET | `/api/categories/{id}` | Herkes | Detay |
| POST/PUT/DELETE | `/api/categories` | ADMIN | Yönetim |

### Ürün
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/products` | Herkes | Sayfalama+filtre+arama, yalnızca `is_active=true` |
| GET | `/api/products/{id}` | Herkes | Detay |
| GET | `/api/seller/products` | SELLER | Giriş yapan satıcının aktif/pasif tüm ürünleri; `Pageable`, varsayılan `createdAt,desc` |
| POST | `/api/seller/products` | SELLER | Ürün ekler (sellerId otomatik atanır) |
| PUT/DELETE | `/api/seller/products/{id}` | SELLER (kendi ürünü) | DELETE = soft delete |
| PUT | `/api/seller/products/{id}/reactivate` | SELLER (kendi ürünü) | Yanlışlıkla silineni geri getirir (`is_active=true`) |
| PUT | `/api/admin/products/{id}/deactivate` | ADMIN | Moderasyon |
| GET | `/api/admin/products` | ADMIN | Aktif/pasif tüm ürünler; `active`, `search`, `categoryId` filtreleri + `Pageable` |

### Adres
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| GET/POST/PUT/DELETE | `/api/addresses` | Giriş yapmış | Kendi adreslerini yönetir |

### Sepet
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| GET | `/api/cart` | Giriş yapmış | Kendi sepeti |
| POST | `/api/cart/items` | Giriş yapmış | Ürün ekler |
| PUT | `/api/cart/items/{itemId}` | Giriş yapmış | Adet günceller |
| DELETE | `/api/cart/items/{itemId}` | Giriş yapmış | Ürün çıkarır |
| DELETE | `/api/cart` | Giriş yapmış | Sepeti boşaltır |

### Sipariş
| Method | Endpoint | Yetki | Açıklama |
|---|---|---|---|
| POST | `/api/orders` | Giriş yapmış (buyer) | Checkout — addressId gerekli, satıcı başına ayrı sipariş oluşur |
| GET | `/api/orders` | Buyer | Kendi siparişleri |
| GET | `/api/orders/{id}` | Buyer (kendi siparişi) | Detay |
| POST | `/api/orders/{id}/cancel` | Buyer | Yalnızca PENDING/CONFIRMED'de |
| GET | `/api/seller/orders` | SELLER | Kendine gelen siparişler |
| PUT | `/api/seller/orders/{id}/status` | SELLER (kendi siparişi) | PENDING→CONFIRMED→SHIPPED→DELIVERED (satıcının yeni siparişi onaylaması da dahil) |
| GET | `/api/admin/orders` | ADMIN | Platform geneli görünüm |
| PUT | `/api/admin/orders/{id}/status` | ADMIN | Her zaman override |

## 6. Haftalık Yol Haritası (9 hafta)

| Hafta | Başlık | Kapsam |
|---|---|---|
| 1 | Foundation & Pipeline | Repo, Postgres, Flyway iskeleti, CORS, logging, health endpoint, CI/CD, boş BE+FE canlı deploy, tam ERD |
| 2 | Auth + Refresh Token + Email Doğrulama | Register/login, access token (memory) + refresh token (httpOnly cookie), rotation, logout, Resend entegrasyonu, email doğrulama, rate limiting |
| 3 | Şifre Sıfırlama | Hafta 2'de kurulan Resend'i yeniden kullanan forgot/reset password akışı |
| 4 | Satıcı Onboarding | Seller entity, başvuru + admin onay/red akışı |
| 5 | Ürün Kataloğu | Category+Product, soft delete, optimistic locking, satıcı CRUD, admin moderasyon |
| 6 | Kategori/Arama/Adres | Arama iyileştirme, Address entity + CRUD |
| 7 | Sepet | Cart/CartItem, stok kontrolü |
| 8 | Checkout | Satıcı başına sipariş ayrımı, adres snapshot, transactional stok düşümü |
| 9 | Sipariş Yönetimi | Alıcı/satıcı/admin sipariş yönetimi, durum makinesi, rate limiting genişletme |

Aşağıda her haftanın tam kırılımı var: backend görevleri, frontend görevleri, "definition of done" (o haftanın bitmiş sayılması için gereken kriter).

### Hafta 1 — Foundation & Pipeline

**Backend:**
- Repo (`ecommerce-clone-api`), Railway Postgres bağlantısı
- Flyway kurulumu (`V1__init.sql`, boş/minimal başlangıç — `ddl-auto: validate`, asla `update` değil)
- CORS config (yalnızca frontend'in Vercel origin'ine izin, env var ile yönetilir)
- Yapılandırılmış logging (Logback + her istek için method/path/status/süre loglayan bir filtre)
- Minimal `GET /api/health` endpoint'i
- GitHub Actions: PR açıldığında `mvn verify` çalışır; `main`'e merge → Railway'e otomatik deploy

**Frontend:**
- Repo (`ecommerce-clone-web`, Vite + React + TS + Tailwind)
- BE'nin health endpoint'ine istek atıp durumu gösteren basit bir sayfa
- GitHub Actions: PR açıldığında `npm run build` (+ lint) çalışır
- Vercel bağlantısı; `main`'e push → otomatik deploy

**Birlikte:** Bu doküman ve `DATABASE_SCHEMA.md`'deki tam ERD bu hafta netleştirilir/doğrulanır.

**DoD:** Boş BE+FE canlıda, birbirleriyle CORS üzerinden konuşabiliyor, PR'da testler otomatik çalışıyor, `main`'e merge otomatik deploy tetikliyor.

### Hafta 2 — Auth + Refresh Token + Email Doğrulama

**Backend:**
- `users` (artık `email_verified` alanıyla) + `refresh_tokens` + `email_verification_tokens` migration
- Resend entegrasyonu (API key env var) — bu haftada kurulur, Hafta 3'te yeniden kullanılır
- `POST /api/auth/register` — kullanıcı `email_verified=false` ile oluşturulur, doğrulama linki mail atılır
- `POST /api/auth/verify-email` — token doğrulanır, `email_verified=true` yapılır
- `POST /api/auth/resend-verification` — eski doğrulama token'ı geçersiz kılınır, yenisi mail atılır
- `POST /api/auth/login` — `email_verified=false` ise reddedilir; başarılıysa access token JSON gövdesinde, refresh token production'da **httpOnly Secure SameSite=None**, localhost'ta **httpOnly SameSite=Lax** cookie olarak döner
- `POST /api/auth/refresh` — cookie'deki refresh token doğrulanır, DB'de **eskisi geçersiz kılınıp yenisi verilir** (rotation), yeni access token JSON'da, yeni refresh cookie olarak set edilir
- `POST /api/auth/logout` — refresh token DB'de `revoked=true` yapılır + cookie temizlenir
- BCrypt ile şifre hash'leme
- CORS config'i `Access-Control-Allow-Credentials: true` ile güncellenir (bkz. §4.1, §4.6)
- `login`/`register` endpoint'lerine rate limiting (bucket4j)
- Admin: bu haftada kod yazılmaz — kayıt olduktan sonra elle DB'den rol değiştirilecek (bkz. §4.6)

**Frontend:**
- Kayıt/giriş formları, "email'ini doğrula" bilgilendirme ekranı, doğrulama linkinden açılan sayfa
- Access token yalnızca memory'de tutulur; tüm API istekleri `credentials: 'include'` ile gider (refresh cookie'sinin gönderilebilmesi için)
- Sayfa açılışında sessizce `/api/auth/refresh` denenip access token yenilenir (kullanıcı F5'te oturumda kalır)
- 401 alan isteklerde otomatik `/api/auth/refresh` deneyip orijinal isteği tekrarlayan bir interceptor
- Logout'ta sunucudaki refresh token da iptal ettirilir

**DoD:** Kullanıcı canlıda kayıt olup mail doğrulama linkine tıklayıp giriş yapabiliyor; doğrulanmamış hesap login olamıyor; access token memory'de, refresh token httpOnly cookie'de; sayfa yenilenince oturum kaybolmuyor; logout gerçekten sunucu tarafında token'ı geçersiz kılıyor; login/register brute-force'a karşı sınırlı.

### Hafta 3 — Şifre Sıfırlama

**Backend:**
- `password_reset_tokens` migration
- `POST /api/auth/forgot-password` — Hafta 2'de kurulan Resend'i kullanır; email sistemde olsun olmasın her zaman 200 döner (enumeration önleme), varsa token üretip mail atar
- `POST /api/auth/reset-password` — token + yeni şifreyi alır, doğrular, şifreyi günceller, token'ı `used=true` yapar

**Frontend:**
- "Şifremi Unuttum" formu
- Mailden gelen linkle açılan "Yeni Şifre Belirle" sayfası

**DoD:** Kullanıcı gerçek bir email alıp linkten şifresini değiştirip yeni şifreyle giriş yapabiliyor.

### Hafta 4 — Satıcı Onboarding

**Backend:**
- `sellers` migration
- `POST /api/sellers/apply` — USER başvurur, `status=PENDING` (reddedilmiş bir kullanıcı tekrar başvurursa aynı satır güncellenir)
- `GET /api/sellers/me` — kullanıcı kendi başvuru durumunu görür
- `GET /api/admin/sellers?status=PENDING` — admin bekleyenleri listeler
- `PUT /api/admin/sellers/{id}/approve` — **tek transaction'da** `Seller.status=APPROVED` + `User.role=SELLER`
- `PUT /api/admin/sellers/{id}/reject` — yalnızca `Seller.status=REJECTED`, rol değişmez

**Frontend:**
- "Satıcı Ol" başvuru formu (mağaza adı, açıklama)
- Başvuru durumu sayfası (PENDING / REJECTED / APPROVED)
- Admin: bekleyen başvurular listesi + onay/red butonları

**DoD:** Bir USER başvurup admin onayıyla gerçekten SELLER oluyor; refresh token mimarisi sayesinde en geç bir sonraki access token yenilemesinde (~20 dk içinde) yeni rol otomatik yansıyor, tam re-login şart değil.

### Hafta 5 — Ürün Kataloğu

**Backend:**
- `categories` migration (yalnızca ADMIN CRUD eder)
- `products` migration (`seller_id`, `category_id`, `image_url`, `is_active`, `version`)
- `POST/PUT/DELETE /api/seller/products` — `findByIdAndSellerId` ile sahiplik kontrolü; DELETE aslında `is_active=false` yapar (soft delete)
- `PUT /api/seller/products/{id}/reactivate` — yanlışlıkla silinen ürünü geri getirir (`is_active=true`)
- `PUT /api/admin/products/{id}/deactivate` — admin moderasyonu
- `GET /api/products` — yalnızca `is_active=true`, pagination + sort + kategori filtresi + isim araması
- `GET /api/products/{id}`

**Frontend:**
- Satıcı "Ürünlerim" sayfası (CRUD, `imageUrl` alanı dahil)
- Herkese açık ürün listeleme/detay sayfası (satıcı adı görünür)
- Admin ürün moderasyon görünümü

**DoD:** Onaylı bir satıcı ürün ekleyip silebiliyor (silinen ürün listede görünmüyor ama satır DB'de duruyor), eşzamanlı iki stok güncellemesi `@Version` sayesinde birbirini ezmiyor (biri hata alıyor).

### Hafta 6 — Kategori, Arama, Adres Yönetimi

**Backend:**
- Arama/filtre iyileştirmesi
- `addresses` migration
- `GET/POST/PUT/DELETE /api/addresses` — kullanıcı yalnızca kendi adreslerini yönetir

**Frontend:**
- Kategori filtre UI, arama kutusu
- "Adreslerim" sayfası (adres ekle/düzenle/sil, varsayılan adres seçme)

**DoD:** Kategoriye göre gezinme + isimle arama çalışıyor, kullanıcı birden fazla adres kaydedip yönetebiliyor.

### Hafta 7 — Sepet

**Backend:**
- `carts` + `cart_items` migration
- `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{itemId}`, `DELETE /api/cart/items/{itemId}`, `DELETE /api/cart`
- Ekleme/güncelleme anında güncel stoğa karşı kontrol

**Frontend:**
- Sepet sayfası — ürünler satıcıya göre gruplanmış gösterilir (checkout'ta kaç ayrı sipariş oluşacağının önizlemesi gibi)
- Miktar güncelleme, sepetten çıkarma

**DoD:** Kullanıcı farklı satıcılardan ürünleri aynı sepette tutup yönetebiliyor, stok sınırı aşılamıyor.

### Hafta 8 — Checkout (Satıcı Başına Sipariş Ayrımı)

**Backend:**
- `orders` + `order_items` migration
- `POST /api/orders`: teslimat adresi seçimi zorunlu → sepet satıcıya göre gruplanır → her satır için `@Version` korumalı fail-fast stok kontrolü (hiçbir şey mutasyona uğramadan önce hepsi kontrol edilir) → **tek transaction içinde** satıcı başına ayrı `Order` (seçilen adresin bilgisi snapshot olarak yazılır) + `OrderItem` oluşturulur → sepet boşaltılır

**Frontend:**
- Checkout sayfası: kayıtlı adreslerden seçim + satıcı bazlı gruplanmış özet ("Satıcı A'dan 2 ürün, Satıcı B'den 1 — 2 ayrı sipariş oluşacak")
- Sipariş onay sayfası (oluşan tüm siparişler listelenir)

**DoD:** Checkout doğru sayıda ayrı sipariş üretiyor, her siparişte adres doğru şekilde snapshotlanmış, eşzamanlı iki checkout birbirinin stok düşümünü bozmuyor, herhangi bir adımda hata olursa tüm işlem geri alınıyor (rollback).

### Hafta 9 — Sipariş Yönetimi

**Backend:**
- `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel` (yalnızca PENDING/CONFIRMED'de, iptalde stok iade edilir)
- `GET /api/seller/orders`, `PUT /api/seller/orders/{id}/status` (yalnızca kendi siparişlerinde, PENDING→CONFIRMED→SHIPPED→DELIVERED — satıcı yeni gelen siparişi de bu endpoint'ten onaylar)
- `GET /api/admin/orders`, `PUT /api/admin/orders/{id}/status` (her zaman override edebilir)
- Rate limiting'in `checkout` ve `forgot-password` endpoint'lerine genişletilmesi (`login`/`register` zaten Hafta 2'de eklenmişti)

**Frontend:**
- "Siparişlerim" sayfası (alıcı)
- Satıcı dashboard'unda "Gelen Siparişler" + durum güncelleme
- Admin genel sipariş görünümü

**DoD:** Üç rol de kendi yetkisi dahilinde siparişleri görüp yönetebiliyor; durum geçiş kuralları (§4.5'teki durum makinesi) backend'de zorlanıyor — örn. satıcı DELIVERED'dan geri dönemez, alıcı SHIPPED sonrası iptal edemez.

## 7. Test Stratejisi

Staj projesindeki (`ecommerce-simulation-api`) alışkanlık burada da devam eder — **her haftanın "definition of done"ına test yazımı dahildir**, ayrı bir "test haftası" yoktur:

- **Unit testler** (`*Test.java`, Surefire ile çalışır) — service katmanı, Repository'ler `@Mock` ile mocklanır, assertion'lar AssertJ ile (`assertThat(...)`). Happy path kadar hata senaryoları da test edilir (stok yetersizse exception, var olmayan kaynak `ResourceNotFoundException`, yetkisiz erişim engellendi mi, vb.).
- **Integration testler** (`*IT.java`, Failsafe ile çalışır) — `@SpringBootTest` + MockMvc, Testcontainers ile gerçek bir PostgreSQL container'ına karşı. Controller, security (rol bazlı erişim, sahiplik izolasyonu — bir kullanıcının başka birinin sepetine/siparişine erişemediği) ve uçtan uca transactional davranış test edilir.
- CI (`mvn verify`) her PR'da hem unit hem integration testleri çalıştırır; testler geçmeden `main`'e merge edilmez.

## 8. Backlog (Kapsam Dışı, İleride Gerekirse Eklenir)

- Redis cache (ürün/kategori listeleme)
- Custom domain (FE/BE aynı kök domain'in alt domain'lerine taşınırsa production cookie politikasını daha sıkı `SameSite=Lax` yapabilme iyileştirmesi)
- Genel güvenlik taraması / hardening turu
- Staging ortamı
- Spring Boot Actuator (health/metrics)
- Sentry (hata izleme)
- Sipariş oluşturmada idempotency/çift-tıklama koruması
- Ürün yorumları/puanlama
- Kupon/indirim sistemi
- Gerçek dosya yükleme servisi (görseller için)
- Ürün varyantları (renk/beden vb. — zaten "ürün unique, tek satıcıya ait" kararıyla kapsam dışı)
- Satıcı askıya alma / banlama (`Seller.status`'e `SUSPENDED` eklenmesi ve admin'in onaylı bir satıcıyı sonradan devre dışı bırakabilmesi)
