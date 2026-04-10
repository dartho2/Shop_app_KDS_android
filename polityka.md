# Polityka Prywatności — Kitchen Display System (KDS)

**Ostatnia aktualizacja:** 10 kwietnia 2026 r.

---

## 1. Informacje ogólne

Niniejsza Polityka Prywatności opisuje zasady przetwarzania danych przez aplikację mobilną **Kitchen Display System (KDS)** (dalej: „Aplikacja"), przeznaczoną wyłącznie do użytku wewnętrznego w lokalach gastronomicznych. Aplikacja działa na urządzeniach z systemem Android i służy personelowi kuchni do zarządzania zamówieniami w czasie rzeczywistym.

Administratorem danych jest podmiot prowadzący lokal gastronomiczny, który wdrożył Aplikację (dalej: „Operator"). Aplikacja nie jest przeznaczona dla konsumentów końcowych — korzystają z niej wyłącznie pracownicy lokalu.

---

## 2. Jakie dane są przetwarzane

### 2.1. Dane zamówień

Aplikacja przetwarza dane zamówień przesyłane w czasie rzeczywistym z systemu kasowego / platformy zamówień online. Dane te obejmują:

- numer zamówienia (wewnętrzny i zewnętrzny),
- listę zamówionych produktów (nazwy, ilości, notatki),
- typ zamówienia (dostawa, odbiór osobisty, na miejscu),
- planowany czas realizacji / czas odbioru,
- źródło zamówienia (np. WooCommerce, checkout),
- imię i nazwisko klienta (jeśli przekazane przez system kasowy),
- numer telefonu klienta (jeśli przekazany przez system kasowy).

### 2.2. Dane uwierzytelniające

W celu nawiązania połączenia z serwerem aplikacja przechowuje lokalnie:

- adres URL serwera API (`tenant key`),
- token JWT sesji pracownika (przechowywany w zaszyfrowanej przestrzeni DataStore).

Dane te nie są udostępniane osobom trzecim.

### 2.3. Dane techniczne i logi

Aplikacja zbiera lokalnie dane diagnostyczne:

- logi zdarzeń (połączenie WebSocket, błędy drukowania, zmiany statusów zamówień),
- informacje o błędach aplikacji przekazywane do usługi **Firebase Crashlytics** (szczegóły poniżej),
- dane o konfiguracji urządzenia (model tabletu, wersja systemu Android) — wyłącznie na potrzeby diagnostyki.

### 2.4. Ustawienia użytkownika

Aplikacja przechowuje lokalnie na urządzeniu preferencje wyświetlania i działania KDS:

- wybrane dźwięki powiadomień,
- ustawienia siatki zamówień, trybów wyświetlania,
- konfiguracja drukarek (adres IP, port),
- ustawienia automatycznego drukowania.

Dane te są przechowywane wyłącznie na urządzeniu i nie są synchronizowane z zewnętrznymi serwerami.

---

## 3. Cel przetwarzania danych

Dane przetwarzane przez Aplikację służą wyłącznie do:

| Cel | Podstawa prawna |
|-----|-----------------|
| Wyświetlania zamówień personelowi kuchni w czasie rzeczywistym | Prawnie uzasadniony interes Operatora (art. 6 ust. 1 lit. f RODO) |
| Zarządzania statusem realizacji zamówień | Prawnie uzasadniony interes Operatora |
| Drukowania etykiet / potwierdzeń zamówień na drukarce kuchennej | Prawnie uzasadniony interes Operatora |
| Powiadamiania personelu o nowych zamówieniach | Prawnie uzasadniony interes Operatora |
| Diagnostyki i usuwania błędów technicznych | Prawnie uzasadniony interes Operatora |

Dane **nie są** wykorzystywane do celów marketingowych, profilowania ani nie są przekazywane do firm trzecich w celach komercyjnych.

---

## 4. Usługi zewnętrzne

### 4.1. Firebase Crashlytics (Google LLC)

Aplikacja korzysta z usługi **Firebase Crashlytics** w celu automatycznego raportowania awarii i błędów. Przy wystąpieniu błędu do serwerów Google mogą być przesyłane:

- informacje o wyjątku / błędzie aplikacji (ślad stosu),
- wersja aplikacji i systemu operacyjnego,
- model urządzenia,
- anonimowe identyfikatory instalacji.

Crashlytics **nie przesyła** danych osobowych klientów (numerów zamówień, danych kontaktowych). Polityka prywatności Google: [https://policies.google.com/privacy](https://policies.google.com/privacy)

### 4.2. Firebase Analytics (Google LLC)

Aplikacja może korzystać z Firebase Analytics do zbierania anonimowych danych o użytkowaniu. Dane są agregowane i nie pozwalają na identyfikację konkretnych osób.

### 4.3. Serwer API Operatora

Aplikacja komunikuje się wyłącznie z serwerem API wskazanym przez Operatora podczas konfiguracji. Wszelkie dane zamówień są przechowywane i przetwarzane po stronie tego serwera zgodnie z polityką prywatności Operatora.

---

## 5. Przechowywanie danych

### 5.1. Lokalne przechowywanie na urządzeniu

- **Baza danych SQLite (Room):** zamówienia są cache'owane lokalnie w celu działania offline i szybkiego wyświetlania. Zamówienia starsze niż **24 godziny** są automatycznie usuwane z lokalnej bazy.
- **DataStore:** preferencje i tokeny sesji przechowywane w zaszyfrowanej przestrzeni Android DataStore.
- **Logi diagnostyczne:** pliki logów przechowywane lokalnie, starsze niż 7 dni są automatycznie usuwane.

### 5.2. Dane na serwerze

Dane zamówień przechowywane na serwerze API Operatora podlegają jego własnej polityce retencji danych.

---

## 6. Bezpieczeństwo danych

Aplikacja stosuje następujące środki ochrony danych:

- komunikacja z serwerem API wyłącznie przez szyfrowane połączenie **HTTPS/TLS**,
- tokeny uwierzytelniające przechowywane w **zaszyfrowanej przestrzeni DataStore** (Android Keystore),
- automatyczne odświeżanie tokenów JWT bez przechowywania hasła na urządzeniu,
- aplikacja przeznaczona wyłącznie do użytku na dedykowanych urządzeniach wewnętrznych — nie jest dostępna publicznie w sklepach aplikacji.

---

## 7. Uprawnienia systemowe

Aplikacja wymaga następujących uprawnień systemu Android:

| Uprawnienie | Cel |
|-------------|-----|
| `INTERNET` | Komunikacja z serwerem API i WebSocket |
| `FOREGROUND_SERVICE` | Utrzymanie połączenia WebSocket w tle |
| `POST_NOTIFICATIONS` | Powiadomienia o nowych zamówieniach |
| `VIBRATE` | Wibracje przy nowych zamówieniach |
| `RECEIVE_BOOT_COMPLETED` | Automatyczne uruchomienie po restarcie urządzenia |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Zapobieganie uśpieniu usługi przez system |
| `SCHEDULE_EXACT_ALARM` | Zaplanowane powiadomienia o zamówieniach |

Aplikacja **nie** żąda dostępu do:

- kamery,
- mikrofonu,
- lokalizacji GPS,
- kontaktów,
- galerii zdjęć / plików osobistych.

---

## 8. Prawa osób, których dane dotyczą

W zakresie danych osobowych (np. danych klientów widocznych w zamówieniach) prawa wynikające z RODO (prawo dostępu, sprostowania, usunięcia, ograniczenia przetwarzania, przenoszenia danych) należy kierować bezpośrednio do Operatora lokalu gastronomicznego, który jest administratorem tych danych.

W celu realizacji swoich praw prosimy o kontakt z Operatorem.

---

## 9. Dane dzieci

Aplikacja przeznaczona jest wyłącznie do użytku profesjonalnego przez personel lokalu gastronomicznego. Nie jest skierowana do dzieci i nie gromadzi świadomie danych dzieci poniżej 16. roku życia.

---

## 10. Zmiany polityki prywatności

Operator zastrzega sobie prawo do aktualizacji niniejszej Polityki Prywatności. O istotnych zmianach użytkownicy zostaną poinformowani przez administratora systemu. Aktualna wersja Polityki jest zawsze dostępna w repozytorium projektu oraz w aplikacji w sekcji Ustawienia.

---

## 11. Kontakt

W sprawach dotyczących prywatności i ochrony danych prosimy o kontakt z Operatorem lokalu gastronomicznego, który wdrożył system KDS.

W sprawach technicznych związanych z aplikacją:
- Repozytorium projektu: **SHOP_APP_KDS**
- Pakiet aplikacji: `com.itsorderkds`

---

*Niniejsza Polityka Prywatności obowiązuje od dnia 10 kwietnia 2026 r.*

