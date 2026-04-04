Chcę rozbudować moduł drukowania w aplikacji tak, aby użytkownik mógł skonfigurować DWIE drukarki:
1) Drukarka kuchnia (Kitchen Printer)
2) Drukarka sala / standardowa (Front/Receipt Printer)

Wymagania funkcjonalne:
- W ustawieniach aplikacji ma być możliwość wybrania osobno drukarki dla kuchni i osobno dla sali.
- Obie drukarki muszą mieć możliwość ustawienia tych samych opcji, które są już zaimplementowane dla aktualnej (jednej) drukarki (np. szerokość papieru, kodowanie, auto-cutter, gęstość, liczba kopii, format nagłówka, itp. — użyj istniejących ustawień i przenieś je na per-drukarka).
- Musi istnieć routing wydruków:
    - Zamówienie / bilecik kuchenny -> drukarka kuchnia
    - Paragon / potwierdzenie / wydruk dla sali -> drukarka sala
    - Jeżeli kuchnia nie jest ustawiona, fallback do drukarki sali (albo pokaż komunikat — zaproponuj sensowną logikę).
- Ustawienia mają być zapisane w trwałym storage (taki jak już używam: SharedPreferences/DataStore/DB — dopasuj do projektu).
- Dodaj migrację ustawień: jeżeli wcześniej była jedna drukarka, po aktualizacji ustaw ją jako “sala/standardowa”, a “kuchnia” zostaw pustą.

Wymagania UX/UI:
- Zaprojektuj ekran ustawień w czytelny sposób:
    - Sekcja “Drukarka sala” i sekcja “Drukarka kuchnia”
    - W każdej: wybór urządzenia + przycisk “Test wydruku”
    - Poniżej w każdej sekcji: rozwijane “Ustawienia zaawansowane” z tymi samymi opcjami co wcześniej
- Jeśli jakaś drukarka nie jest wybrana, pokaż stan “Nie skonfigurowano” + CTA “Wybierz drukarkę”
- Upewnij się, że UI dobrze wygląda na tablecie (Material 3), i jest spójne z resztą aplikacji.

Zadanie dla Ciebie:
- Zaproponuj strukturę danych na konfigurację dwóch drukarek (modele/DTO).
- Zrefaktoruj istniejący kod z jednej drukarki na obsługę dwóch (bez duplikowania logiki).
- Wydziel wspólną logikę ustawień drukarki (np. PrinterSettings) i zrób dwa profile: kitchenSettings i frontSettings.
- Dodaj przykładowe funkcje: printKitchenTicket(order), printFrontReceipt(order), oraz testPrint(kitchen/front).
- Pokaż przykładowy kod UI (Compose / XML — zgodnie z tym co jest w projekcie) oraz jak to spiąć z aktualnym ViewModel/Repo.

Pracuj na istniejących klasach i nazwach z projektu (wyszukaj obecną implementację “PrinterSettings” / “PrinterConfig” / “PrintService” itp.) i przerób je zamiast pisać wszystko od zera.
