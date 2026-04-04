# 🧪 TESTING CHECKLIST - Drukarka Kuchenna

## ✅ UI & KONFIGURACJA

### Sekcja Drukarki Frontowej
- [ ] Widać przycisk "Wybierz drukarkę Bluetooth"
- [ ] Widać profil drukarki
- [ ] Widać szablon wydruku
- [ ] Widać encoding
- [ ] Widać checkbox "Obcinaj papier"

### Sekcja Drukarki Kuchennej
- [ ] Widać przełącznik "Włącz drukarkę kuchenną"
- [ ] Po włączeniu pojawia się karta ze statusem
- [ ] Przycisk "Wybierz" - wybór urządzenia BT
- [ ] Przycisk "Test wydruku" - test drukarki
- [ ] Checkbox "Obcinaj papier (kuchnia)"

---

## 🔧 PROFIL DRUKARKI

### Drukarka Frontowa
- [ ] Kliknij na profil
- [ ] Widać listę: YHD-8390, Mobile, Niestandardowy
- [ ] Wybierz YHD-8390 → ustawi CP852
- [ ] Wybierz Mobile → ustawi UTF-8
- [ ] Wybierz Niestandardowy → pojawi się dialog

### Drukarka Kuchenna
- [ ] Po włączeniu i wyborze BT pojawia się komponent profilu
- [ ] Kliknij na profil
- [ ] Widać listę profili
- [ ] Wybierz YHD-8390 → ustawi CP852
- [ ] Test wydruku → drukuje na kuchni z CP852

---

## 📋 SZABLON WYDRUKU

### Drukarka Frontowa
- [ ] Kliknij na szablon
- [ ] Widać: STANDARD, COMPACT, DETAILED, MINIMAL
- [ ] Wybierz STANDARD → zapisze się
- [ ] Po zaakceptowaniu zamówienia drukuje się STANDARD template

### Drukarka Kuchenna
- [ ] Po włączeniu pojawia się sekcja szablonu
- [ ] Kliknij na szablon
- [ ] Widać listę szablonów
- [ ] Wybierz COMPACT → zapisze się dla kuchni
- [ ] Bilecik kuchenny drukuje się w COMPACT

---

## 🔤 ENCODING & CODEPAGE

### Drukarka Frontowa
- [ ] Aktualny encoding wyświetla się
- [ ] Profil YHD-8390 → CP852 + codepage 13
- [ ] Profil Mobile → UTF-8 + brak codepage
- [ ] Niestandardowy → dialog do wpisania

### Drukarka Kuchenna
- [ ] Po włączeniu wyświetla się encoding
- [ ] Profil YHD-8390 → CP852 + codepage 13
- [ ] Polskie znaki drukują się poprawnie
- [ ] Niestandardowy ��� dialog do wpisania

---

## 🖨️ TEST WYDRUKU

### Drukarka Frontowa
- [ ] Przycisk "Drukuj stronę testową" (front)
- [ ] Drukuje: "Strona testowa", polskie znaki
- [ ] Tekst widoczny i czytelny

### Drukarka Kuchenna
- [ ] Włącz drukarkę kuchenną
- [ ] Wybierz urządzenie BT
- [ ] Kliknij "Test wydruku"
- [ ] Drukuje na drukarce kuchennej
- [ ] Polskie znaki OK
- [ ] Auto-cut działa (jeśli włączony)

---

## 📩 AUTO-DRUK PO AKCEPTACJI

### Konfiguracja
- [ ] Włącz "Automatyczny wydruk po zaakceptowaniu"
- [ ] Włącz drukarkę kuchenną
- [ ] Skonfiguruj obie drukarki

### Test
- [ ] Odbierz zamówienie PENDING
- [ ] Kliknij ACCEPT ORDER
- [ ] Czekaj 2-3 sekundy
- [ ] Drukuje się bilecik na KUCHNI
- [ ] Drukuje się paragon na FRONCIE

### Fallback (brak drukarki kuchennej)
- [ ] Wyłącz drukarkę kuchenną
- [ ] Zaakceptuj zamówienie
- [ ] Bilecik drukuje się tylko na FRONCIE

---

## 🔧 KONFIGURACJA ZAAWANSOWANA

### Niestandardowy Encoding
- [ ] Frontowa: Kliknij profil → Niestandardowy → Dialog
- [ ] Wpisz kodowanie (np. "CP850") i codepage (np. "15")
- [ ] Zapisz
- [ ] Ustawi się dla drukarki frontowej
- [ ] Kuchnia: Ten sam proces

### Profile
- [ ] YHD-8390 → CP852, codepage 13
- [ ] Mobile → UTF-8, brak codepage
- [ ] Inne → spróbuj niestandardowy

---

## ✂️ AUTO-CUT

### Drukarka Frontowa
- [ ] Checkbox "Obcinaj papier po wydruku"
- [ ] Po zaznaczeniu - papier się cina
- [ ] Po odznaczeniu - papier się nie cina

### Drukarka Kuchenna
- [ ] Checkbox "Obcinaj papier po wydruku (kuchnia)"
- [ ] Test wydruku → papier się cina
- [ ] Po zaakceptowaniu zamówienia → papier się cina

---

## 📊 RAPORT Z TESTÓW

### Przed Testami
- [ ] Liczba błędów kompilacji: ___
- [ ] Czas kompilacji: ___

### Po Testach
- [ ] Wszystkie testy UI: ✅ / ❌
- [ ] Profile drukarki: ✅ / ❌
- [ ] Szablony: ✅ / ❌
- [ ] Encoding: ✅ / ❌
- [ ] Auto-druk: ✅ / ❌
- [ ] Polskie znaki: ✅ / ❌

### Problemy Znalezione
- [ ] Brak problemów
- [ ] Znalezione problemy:
  1. ...
  2. ...
  3. ...

---

## 📝 NOTATKI

_Miejsce na notatki z testów_

...

---

**Data Testu:** ___________  
**Tester:** ___________  
**Status:** ⏳ Oczekiwanie / ✅ OK / ❌ Błędy

