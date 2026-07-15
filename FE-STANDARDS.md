# Frontend Standartları — E-Commerce Marketplace Clone

> **Versiyon:** 1.0
> **Kaynak:** `PROJECT_PLAN.md` ve `DATABASE_SCHEMA.md` (bu dizinde)
> **Durum:** Aktif

Bu doküman, bu projenin frontend'i için mimari kararları ve kodlama standartlarını tek doğruluk kaynağı olarak tanımlar. Frontend'i Claude yazıp sürdürüyor (bkz. `PROJECT_PLAN.md` §3 "Sorumluluk paylaşımı"); araç seçimleri (state yönetimi, form kütüphanesi vb.) bu kapsamda Claude tarafından belirlenmiştir.

---

## İçindekiler

1. [Mimari](#1-mimari)
2. [State & Data Fetching](#2-state--data-fetching)
3. [Auth Akışı](#3-auth-akışı)
4. [Routing](#4-routing)
5. [Form & Validasyon](#5-form--validasyon)
6. [Styling](#6-styling)
7. [Tip Güvenliği](#7-tip-güvenliği)
8. [Test Stratejisi](#8-test-stratejisi)
9. [Git & Commit Standartları](#9-git--commit-standartları)
10. [Anti-Pattern Referans Tablosu](#10-anti-pattern-referans-tablosu)
11. [Değiştirilemez Kurallar](#11-değiştirilemez-kurallar-özet)
12. [Tech Stack](#12-tech-stack)
13. [Kullanılmayan Ağır Yapılar](#13-kullanılmayan-ağır-yapılar)

---

## 1. Mimari

- Vite + React + TypeScript, tek sayfa uygulaması (SPA), **tek repo/tek deploy**.
- Admin paneli ve satıcı paneli **ayrı uygulamalar değil** — aynı app içinde role'e göre kapılı rota grupları (`/admin/*`, `/seller/*`, geri kalanı alıcı deneyimi).
- Rota koruması yalnızca kullanıcı deneyimi içindir; **gerçek yetkilendirme her zaman backend'e aittir** — FE hiçbir zaman "yetkim var" varsayımıyla veri göstermez, her zaman API'nin 401/403 cevabına güvenir.

## 2. State & Data Fetching

- Sunucu verisi (API çağrıları, cache, mutation, yeniden deneme) için **TanStack Query (React Query)**.
- İstemci-yerel durum (giriş yapmış kullanıcı bilgisi, sepet önizleme grupları) için **React Context + hooks** — proje ölçeği bir global state kütüphanesi (Redux/Zustand) gerektirmiyor.
- HTTP istekleri native `fetch` ile yapılır; axios gibi ekstra bir kütüphane eklenmez.

## 3. Auth Akışı

- Access token **yalnızca memory'de** (React state/context) tutulur — localStorage/sessionStorage asla kullanılmaz.
- Her istek `credentials: 'include'` ile gider (refresh cookie'sinin gönderilebilmesi için — bkz. `BE-STANDARDS.md` §3).
- Uygulama açılışında sessizce `/api/auth/refresh` denenir — kullanıcı sayfa yenilendiğinde (F5) oturumda kalır.
- 401 alan bir istekte otomatik olarak bir kez refresh denenir, başarılıysa orijinal istek tekrarlanır; başarısızsa kullanıcı login'e yönlendirilir (sonsuz döngüye girilmez).

## 4. Routing

- **React Router** — role bazlı route guard'lar (`RequireRole` gibi bir wrapper component).
- Satıcı-ilişkili ekranlarda 4 durumlu dallanma route seviyesinde yönetilir: hiç başvurmamış USER / PENDING / REJECTED / APPROVED (bkz. `PROJECT_PLAN.md` §4.7).

## 5. Form & Validasyon

- **React Hook Form** ile form state yönetimi.
- Hata mesajları backend'in `ErrorResponse.fieldErrors`'ından doğrudan yansıtılır — FE kendi paralel bir validasyon mesajı metni icat etmez, backend'le tutarlı kalır.

## 6. Styling

- **Tailwind CSS**, component-level utility class'lar. Ayrı bir CSS-in-JS kütüphanesi kullanılmaz.

## 7. Tip Güvenliği

- API response şekilleri, backend DTO'larıyla birebir eşleşen TypeScript interface'leri olarak tanımlanır.
- `any` kullanımı yasak; tip belirsizse `unknown` + daraltma (type narrowing).

## 8. Test Stratejisi

- Bu projede FE için otomatik bir test paketi **bilinçli olarak kapsam dışı** bırakıldı — backend'deki gibi bir unit/integration test kültürü FE'de yok. Claude her özelliği geliştirdikten sonra dev sunucusu üzerinden manuel/görsel olarak doğrular.
- İleride gerekirse Vitest + React Testing Library eklenebilir (backlog, `PROJECT_PLAN.md` §8'e taşınabilir).

## 9. Git & Commit Standartları

Backend ile aynı desen (bkz. `PROJECT_PLAN.md` "Git & Deploy Workflow"):
- Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`).
- `main`'den `feature/<isim>` branch'i, PR ile merge.
- CI (`npm run build` + lint) yeşil olmadan PR merge edilmez.

## 10. Anti-Pattern Referans Tablosu

| ❌ Yanlış | ✅ Doğru |
|---|---|
| Access/refresh token'ı localStorage'da tutmak | Access memory'de, refresh httpOnly cookie'de |
| `credentials` olmadan `fetch` çağırmak | Her istekte `credentials: 'include'` |
| Admin/satıcı panelini ayrı bir app olarak kurmak | Aynı app, role bazlı route guard |
| FE'de "yetkim var" varsayımıyla veri göstermek | Backend'in 401/403 cevabına güvenmek |
| `any` tipi | Backend DTO'suyla eşleşen interface |
| Backend hata mesajını FE'de yeniden yazmak | `ErrorResponse.fieldErrors`'ı doğrudan göstermek |
| Yeni bir global state kütüphanesi eklemek | React Query + Context yeterli olduğu sürece eklenmez |
| 401'de sonsuz refresh döngüsü | Tek seferlik refresh + başarısızsa login'e yönlendirme |

## 11. Değiştirilemez Kurallar (Özet)

| Kural | Durum |
|---|---|
| Access/refresh token localStorage'da | ❌ Yasak |
| `credentials: 'include'` olmadan auth gerektiren istek | ❌ Yasak |
| Admin/satıcı paneli için ayrı bir uygulama açmak | ❌ Yasak |
| FE'nin kendi başına yetki/rol kararı vermesi | ❌ Yasak |
| `any` tipi kullanımı | ❌ Yasak |
| CI kırmızıyken merge etmek | ❌ Yasak |

## 12. Tech Stack

*(Şu an bu projede kullandığımız teknolojiler — alfabetik sıra)*

Conventional Commits · ESLint · GitHub Actions · React · React Hook Form · React Router · Tailwind CSS · TanStack Query (React Query) · TypeScript · Vercel · Vite

## 13. Kullanılmayan Ağır Yapılar

*(Bu projede bilinçli olarak kullanılmayan/tercih edilmeyen ağır yapılar — alfabetik sıra. Yeni bir şey eklemeden önce burada olup olmadığına bak.)*

CSS-in-JS (styled-components / Emotion) · GraphQL Client (Apollo/urql) · Micro-frontend Mimarisi · Native Mobile (React Native) · Next.js (SSR/SSG Framework) · Redux / Zustand (Global State Kütüphanesi) · Server-Side Rendering (SSR) · WebSocket / Real-time İletişim

---

> **Doküman Sonu** — Sorular için `PROJECT_PLAN.md`'ye bakın ya da Claude ile konuşun.
