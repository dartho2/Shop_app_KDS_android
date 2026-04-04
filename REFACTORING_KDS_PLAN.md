# Plan refaktoryzacji aplikacji na dedykowaną KDS

## Data: 2026-04-04
## Cel: Przekształcenie aplikacji wielofunkcyjnej na dedykowaną aplikację Kitchen Display System

---

## ETAP 1: Analiza i przygotowanie ✓
- [✓] Zmiana package name z com.itsorderchat na com.itsorderkds
- [✓] Weryfikacja buildu

---

## ETAP 2: Usunięcie niepotrzebnych modułów UI

### Do usunięcia:
- [ ] `/ui/product/` - zarządzanie produktami
- [ ] `/ui/vehicle/` - zarządzanie pojazdami
- [ ] `/ui/routes/` - zarządzanie trasami dostaw
- [ ] `/ui/category/` - kategorie produktów
- [ ] `/ui/open/` - godziny otwarcia (jeśli nie używane)
- [ ] `ProductActivity.kt`
- [ ] `PermissionsFragment.kt` (jeśli związany z lokalizacją)

### Do zachowania:
- [✓] `/ui/order/` - główny moduł wyświetlania zamówień (KDS tickets)
- [✓] `/ui/auth/` - logowanie
- [✓] `/ui/login/` - ekran logowania
- [✓] `/ui/settings/` - ustawienia (drukarki, dźwięki)
- [✓] `/ui/theme/` - motywy i główna aktywność
- [✓] `/ui/dialog/` - dialogi
- [✓] `/ui/language/` - obsługa języków

---

## ETAP 3: Czyszczenie API i Repositories

### Do usunięcia:
- [ ] `ProductApi.kt` i `ProductRepository.kt`
- [ ] `VehicleApi.kt` i `VehicleRepository.kt`
- [ ] `CategoryApi.kt` (jeśli istnieje)
- [ ] Endpoint-y związane z produktami w `ApiService.kt`

### Do zachowania:
- [✓] `OrderApi.kt` - pobieranie zamówień (KDS tickets)
- [✓] `AuthApi.kt` - autentykacja
- [✓] `SettingsApi.kt` - ustawienia
- [✓] `WebSocketProvider.kt` - Socket.IO dla real-time KDS

---

## ETAP 4: Aktualizacja modeli danych

### Do dodania (zgodnie z dokumentacją KDS.md):
- [ ] `KdsTicket.kt` - model ticketu KDS
- [ ] `KdsTicketItem.kt` - model pozycji ticketu
- [ ] `KdsStation.kt` - model stanowiska kuchni
- [ ] `KdsAudit.kt` - model audytu akcji
- [ ] Stany: `KdsTicketState`, `KdsItemState`

### Do usunięcia/uproszczenia:
- [ ] Pola związane z dostawą w `Order.kt` (jeśli nie używane)
- [ ] Modele tras i pojazdów

---

## ETAP 5: Aktualizacja navigation

### Do usunięcia z nav_graph:
- [ ] `productsFragment`
- [ ] Fragmenty związane z trasami/dostawami

### Do zachowania:
- [✓] `homeFragment` - główny ekran z ticketami KDS
- [✓] `SettingsFragment` - ustawienia
- [✓] `loginFragment` - logowanie

---

## ETAP 6: Aktualizacja AndroidManifest

### Do usunięcia:
- [ ] Uprawnienia związane z lokalizacją GPS (jeśli nie używane)
- [ ] ProductActivity (jeśli istnieje w manifeście)

### Do zachowania:
- [✓] Podstawowe uprawnienia (INTERNET, NOTIFICATIONS)
- [✓] Uprawnienia Bluetooth (dla drukarek)
- [✓] HomeActivity, LoginActivity, MainActivity

---

## ETAP 7: Implementacja endpointów KDS API

### Nowe endpointy (zgodnie z KDS.md):
- [ ] GET `/staff/kds/tickets` - lista ticketów
- [ ] GET `/staff/kds/tickets/:id` - szczegóły ticketu
- [ ] POST `/staff/kds/tickets/:id/ack` - potwierdź
- [ ] POST `/staff/kds/tickets/:id/start` - rozpocznij
- [ ] POST `/staff/kds/tickets/:id/ready` - gotowe
- [ ] POST `/staff/kds/tickets/:id/handoff` - wydaj
- [ ] POST `/staff/kds/tickets/:id/cancel` - anuluj
- [ ] POST `/staff/kds/items/:id/start` - start pozycji
- [ ] POST `/staff/kds/items/:id/ready` - pozycja gotowa
- [ ] GET `/staff/kds/stations` - lista stanowisk

---

## ETAP 8: Socket.IO - eventy KDS

### Do dodania:
- [ ] Nasłuchiwanie namespace `/staff`
- [ ] Event `KDS_TICKET_CREATED`
- [ ] Event `KDS_TICKET_ACKED`
- [ ] Event `KDS_TICKET_STARTED`
- [ ] Event `KDS_TICKET_READY`
- [ ] Event `KDS_TICKET_HANDOFF`
- [ ] Event `KDS_TICKET_CANCEL`
- [ ] Event `KDS_ITEM_STARTED`
- [ ] Event `KDS_ITEM_READY`

---

## ETAP 9: UI/UX dla KDS

### Do zaimplementowania:
- [ ] Widok kart ticketów (grid/lista)
- [ ] Kolorowanie według SLA (zielony/żółty/czerwony)
- [ ] Przyciski akcji: ACK, START, READY, HANDOFF, CANCEL
- [ ] Widok pozycji w tickecie
- [ ] Timer/licznik czasu dla każdego ticketu
- [ ] Filtry: NEW, ACKED, IN_PROGRESS, READY
- [ ] Dźwięk alarmu przy nowym tickecie

---

## ETAP 10: Czyszczenie zasobów

### Do usunięcia:
- [ ] Layout-y produktów
- [ ] Layout-y tras/dostaw
- [ ] Niepotrzebne drawable
- [ ] Niepotrzebne stringi w resources

---

## ETAP 11: Testy i walidacja

- [ ] Test logowania
- [ ] Test pobierania ticketów
- [ ] Test zmiany stanów
- [ ] Test Socket.IO połączenia
- [ ] Test drukowania ticketów
- [ ] Test obsługi wielu tabletów jednocześnie

---

## ETAP 12: Dokumentacja i cleanup

- [ ] Aktualizacja README.md
- [ ] Cleanup kodu (unused imports, variables)
- [ ] Aktualizacja build.gradle (usunięcie niepotrzebnych zależności)
- [ ] Finalne testy


