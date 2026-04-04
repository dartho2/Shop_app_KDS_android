Popraw cały moduł drukowania, bo teraz wszystko drukuje na pierwszej drukarce.

Wymagania:
1) Muszą być 2 logiczne drukarki:
   - STANDARD (rachunek/paragon)
   - KITCHEN (kuchnia)
2) Każda z nich ma mieć swoje osobne ustawienia (adres/port/typ/sterownik itp.) i drukowanie ma wybierać drukarkę na podstawie tych ustawień.
3) NIE powielaj kodu od ustawień. Zrób jedną wspólną implementację konfiguracji, a różnica ma wynikać tylko z typu drukarki (STANDARD/KITCHEN) i zapisanych danych.
4) UI:
   - Ekran/Lista pokazuje 2 pozycje: "Drukarka standardowa" i "Drukarka kuchenna"
   - Po kliknięciu w pozycję przechodzę do ekranu ustawień tej drukarki (ten sam ekran/komponent, ale ładuje dane dla wybranego typu).
5) Dane:
   - Zrób model np. PrinterType { STANDARD, KITCHEN }
   - Zrób model PrinterSettings (wspólny dla obu)
   - Zapis/odczyt ustawień per typ (np. DataStore/SharedPreferences/Room – użyj tego co już jest w projekcie)
6) Drukowanie:
   - Faktura/rachunek/paragon zawsze idzie na STANDARD
   - Wydruk kuchenny (produkty kuchenne) zawsze idzie na KITCHEN
   - Jeżeli KITCHEN nie skonfigurowana → pokaż czytelny błąd/komunikat i nie drukuj na STANDARD “fallbackiem” bez mojej zgody.
7) Zrefaktoruj aktualny kod tak, aby wybór drukarki NIE był "pierwsza z listy", tylko był deterministyczny na podstawie PrinterType.

Wynik:
- Konkretny refactor w kodzie: modele, storage ustawień, ekran listy 2 drukarek, ekran ustawień, oraz zmiana logiki print(), aby wybierała właściwą drukarkę.
- Bez duplikowania kodu ustawień/ekranów dla standard i kitchen.
