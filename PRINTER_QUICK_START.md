# 🖨️ System Dwóch Drukarek - Quick Start

## ✅ Co zostało dodane?

Aplikacja teraz obsługuje **2 drukarki Bluetooth**:
- **Drukarka FRONT (sala)** – paragony, potwierdzenia
- **Drukarka KITCHEN (kuchnia)** – bileciki kuchenne

---

## 🚀 Jak Skonfigurować?

### 1. Wejdź w Ustawienia
**Menu → Ustawienia → Drukarka**

### 2. Skonfiguruj Drukarkę Frontową (pierwsza sekcja)
1. Kliknij **"Wybierz drukarkę Bluetooth"**
2. Wybierz urządzenie (np. InnerPrinter, POS-8390)
3. Wybierz **profil drukarki**:
   - YHD-8390 → **CP852** (polskie znaki)
   - Mobile → **UTF-8**
4. Zaznacz **"Obcinaj papier"** (jeśli drukarka ma cutter)

### 3. Skonfiguruj Drukarkę Kuchenną (na dole ekranu)
1. Włącz przełącznik **"Włącz drukarkę kuchenną"**
2. Kliknij **"Wybierz"** → wybierz urządzenie BT
3. Kliknij **"Drukuj stronę testową"** aby sprawdzić
4. Zaznacz **"Obcinaj papier (kuchnia)"** jeśli potrzebne

### 4. Włącz Auto-Druk Po Akceptacji
**Sekcja "Automatyzacja":**
- Zaznacz **"Automatyczny wydruk po zaakceptowaniu"**

---

## 📝 Jak To Działa?

### Po zaakceptowaniu zamówienia:
1. **Bilecik kuchenny** → drukuje się na **drukarce kuchennej**
   - Jeśli kuchnia wyłączona → drukuje na **froncie**
2. **Paragon** → drukuje się na **drukarce frontowej**

### Ręczne Drukowanie:
- **Ikona drukarki** przy zamówieniu → drukuje na **froncie**

---

## 🧪 Test

1. Kliknij **"Drukuj stronę testową"** w sekcji frontu
2. Kliknij **"Drukuj stronę testową"** w sekcji kuchni
3. Zaakceptuj zamówienie testowe → sprawdź czy drukują się oba bileciki

---

## ❓ FAQ

**Q: Mam tylko jedną drukarkę, co zrobić?**  
A: Zostaw drukarkę kuchenną wyłączoną (przełącznik OFF). Wszystko będzie drukować na froncie.

**Q: Polskie znaki wyświetlają się jako krzaki**  
A: Ustaw profil **YHD-8390** (CP852) w sekcji drukarki frontowej.

**Q: Nie tnie papieru**  
A: Zaznacz checkbox **"Obcinaj papier po wydruku"** w Ustawieniach.

**Q: Kuchnia nie drukuje**  
A: Sprawdź:
- Czy przełącznik "Włącz drukarkę kuchenną" jest ON
- Czy wybrano urządzenie BT
- Czy test wydruku działa

---

## 📚 Dokumentacja Techniczna

Pełna dokumentacja: `PRINTER_IMPLEMENTATION.md`  
Podsumowanie zmian: `PRINTER_DUAL_SETUP_COMPLETE.md`

---

**Status:** ✅ Gotowe do użycia  
**Data:** 2026-01-21

