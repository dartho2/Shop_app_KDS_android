KDS — Dokumentacja Techniczna (wersja zoptymalizowana pod nasze rozwiązanie)

ItsOrder Kitchen Display System (KDS)
Wersja dokumentu: zoptymalizowana pod sushi / bar / wydawkę
Android min SDK 26 | Target SDK 35
Pakiet: com.itsorderkds

1. Cel systemu

ItsOrder KDS to aplikacja Android dla kuchni, której celem jest zastąpienie papierowych bloczków na etapie przygotowania zamówień, przy zachowaniu prostego procesu pracy.

System ma działać w restauracji tak, aby:

kuchnia widziała wszystkie zaakceptowane zamówienia na ekranie,
kucharz nie musiał czytać papierowych kartek,
po przygotowaniu zamówienia system informował obsługę, że zamówienie jest gotowe,
wydruk bilecika następował dopiero po oznaczeniu zamówienia jako gotowe, a nie przy samym przyjęciu.

To podejście ma zmniejszyć chaos, ograniczyć składowanie toreb z bloczkami i uprościć pracę przy sushi / barze / wydawce.

2. Główny model pracy
   2.1 Aktualny model, który chcemy zastąpić

Obecnie:

kelner przyjmuje zamówienie,
drukuje bloczek,
bloczek trafia do kuchni albo do torby,
kucharz czyta bloczek i przygotowuje zamówienie.

To powoduje:

bałagan z papierem,
konieczność fizycznego przekładania bloczków,
gorszą kontrolę nad statusem zamówienia,
brak wygodnej informacji „co już gotowe”.
2.2 Docelowy model

Docelowo:

zamówienie wpada do POS / integracji,
kelner lub system je akceptuje,
zaakceptowane zamówienie pojawia się na KDS,
kuchnia widzi zamówienie na ekranie,
po przygotowaniu kucharz oznacza zamówienie jako gotowe,
system:
aktualizuje status,
pokazuje kelnerowi / wydawce, że zamówienie jest gotowe,
opcjonalnie automatycznie drukuje bilecik do przypięcia do torby lub wydania.

W tym modelu papier pojawia się dopiero na końcu procesu, gdy zamówienie jest gotowe.

3. Podział odpowiedzialności
   3.1 Kelner / POS

Kelner pracuje głównie na POS i:

przyjmuje zamówienia,
akceptuje je do realizacji,
może oznaczyć zamówienie jako pilne,
widzi listę zamówień gotowych do wydania.

Kelner nie powinien wykonywać w KDS pełnej obsługi przygotowania.

3.2 Kuchnia / sushi master

Kuchnia pracuje głównie na KDS i:

widzi zamówienia do przygotowania,
przygotowuje je,
oznacza je jako gotowe.

W uproszczonym trybie kuchnia nie musi klikać „przyjęte” ani „w przygotowaniu”, jeśli to nie daje wartości operacyjnej.

3.3 Wydawka / pakowanie

Po oznaczeniu zamówienia jako gotowe:

kelner / wydawka dostaje informację,
opcjonalnie drukuje się bloczek,
zamówienie jest pakowane albo wydawane gościowi / kurierowi.
4. Tryby działania KDS

System powinien obsługiwać dwa tryby pracy, konfigurowane w ustawieniach.

4.1 Tryb prosty — rekomendowany dla sushi / baru

To główny i domyślny tryb.

Stany:
ACTIVE — zamówienie aktywne / do przygotowania
READY — zamówienie gotowe
CANCELLED — anulowane
Logika:
po akceptacji przez kelnera / POS zamówienie trafia na KDS jako ACTIVE
kucharz widzi zamówienie i po zakończeniu klika GOTOWE
system zmienia status na READY
opcjonalnie uruchamia wydruk

Ten tryb minimalizuje klikanie i najlepiej pasuje do pracy, gdzie kucharz ma brudne ręce i nie chce obsługiwać wielu statusów.

4.2 Tryb kolejkowy — opcjonalny

Dla lokali, które chcą pełniejszy workflow.

Stany:
NEW
ACKED
IN_PROGRESS
READY
HANDED_OFF
CANCELLED
Logika:
używany tylko wtedy, gdy lokal rzeczywiście potrzebuje etapów pośrednich
można go włączyć w ustawieniach
4.3 Ustawienie trybu

W aplikacji należy dodać konfigurację:

queueMode = false → tryb prosty
queueMode = true → tryb kolejkowy
5. Kluczowa decyzja UX
   5.1 Kucharz ma klikać minimum

Dla Waszego przypadku najważniejsze jest:

nie mnożyć statusów bez potrzeby,
nie wymagać od kucharza wielu dotknięć ekranu,
umożliwić w przyszłości obsługę przez fizyczne przyciski Bluetooth.
5.2 Główna akcja kuchni

Najważniejsza akcja na KDS to:

GOTOWE

Opcjonalnie można dodać:

ANULUJ
COFNIJ GOTOWE

Ale główny przepływ powinien być maksymalnie prosty.

6. Widok główny
   6.1 Założenie

KDS ma wyświetlać siatkę bloczków zamówień, czytelną z większej odległości.

Każdy bloczek powinien zawierać:

numer zamówienia,
typ: na miejscu / odbiór / dostawa,
godzinę planowaną,
czas od przyjęcia,
priorytet,
pozycje zamówienia,
uwagi,
status.
6.2 Układ

Zamiast poziomego LazyRow jako głównego układu lepiej przejść na stabilny układ siatkowy / kolumnowy.

Na tabletach:

10" → zwykle 2 kolumny
12" → zwykle 3 kolumny
większe ekrany → 3–4 kolumny
6.3 Stabilność układu

To jest krytyczne.

Po zniknięciu jednego zamówienia:

reszta nie może chaotycznie przeskakiwać,
użytkownik nie może „zgubić” wzrokiem swojego bloczka.

Dlatego zalecane są 2 podejścia:

stała wysokość kart z ewentualnym przewijaniem treści wewnątrz,
albo
stabilne kolumny, gdzie nowe zamówienia dopisywane są logicznie na końcu.

Nie zaleca się agresywnego przeliczania całego masonry layout po każdej zmianie.

7. Zachowanie bloczków
   7.1 Zmienna długość zamówień

Niektóre zamówienia mają 2 pozycje, inne 10.
Żeby zachować stabilność, lepiej:

ustalić maksymalną wysokość karty,
przy dłuższych zamówieniach pokazać skróconą listę + przewijanie lub rozwinięcie.
7.2 Po oznaczeniu „gotowe”

Po kliknięciu GOTOWE:

bloczek znika z listy aktywnych,
ale układ nie powinien powodować nagłego przetasowania całego ekranu,
nowe zamówienia nie powinny „wpychać się” losowo pomiędzy istniejące.
7.3 Zaznaczenie aktywnego bloczka

Jeśli wprowadzimy fizyczne przyciski, ekran powinien mieć pojęcie:

„aktualnie zaznaczonego zamówienia”.

Wybrany bloczek powinien być wyraźnie podświetlony.

8. Priorytety i pilne zamówienia

Kelner / POS może oznaczyć zamówienie jako pilne, np. gdy:

kurier już czeka,
klient jest na miejscu,
zamówienie trzeba zrobić szybciej.

KDS powinien to pokazać:

wyraźnym kolorem,
ikoną,
ewentualnie przeniesieniem wyżej w kolejce.

Jednocześnie kuchnia powinna nadal mieć kontrolę, żeby nie rozwalać całego procesu.

9. Zamówienia zaplanowane

Zamówienia z scheduledFor mają działać prosto:

jeśli do realizacji jest więcej niż 60 min → ukryte w zakładce „Zaplanowane”
jeśli do realizacji zostało 60 min lub mniej → trafiają do widoku aktywnego
jeśli czas realizacji minął → traktowane jak zwykłe aktywne

Dla takich zamówień system może wyświetlać:

godzinę realizacji,
licznik czasu do rozpoczęcia,
ostrzeżenie „zacznij przygotowanie”.

W trybie prostym nie trzeba wymuszać dodatkowego stanu pośredniego.

10. Grupowanie podobnych pozycji

To jest dla Was bardzo ważne.

Jeśli w podobnym czasie wpada wiele zamówień z tymi samymi zestawami, system powinien docelowo umieć pokazać pomocnicze agregacje, np.:

5x Zestaw A
3x California
2x Futomak pieczony

To nie zastępuje pojedynczych bloczków, ale daje kuchni dodatkowy widok produkcyjny, żeby można było robić rzeczy hurtowo.

10.1 Rekomendacja

Dodać osobny moduł / panel:

Production Summary
liczony na podstawie aktywnych zamówień z podobnym scheduledFor / oknem czasowym

Nie mieszać tego bezpośrednio z podstawowym workflow bloczków.

11. Fizyczne przyciski

KDS ma być przygotowany do pracy z:

zewnętrznymi przyciskami Bluetooth,
mini kontrolerem HID,
pilotem Bluetooth,
w przyszłości także dedykowanym panelem 4–5 przycisków.
11.1 Minimalny zestaw akcji
NEXT
PREVIOUS
CONFIRM
CANCEL
11.2 Proponowany workflow
użytkownik przechodzi między bloczkami przyciskami lewo/prawo
wybiera zamówienie
klika OK
pojawia się lekkie potwierdzenie:
OK
ANULUJ
po potwierdzeniu zamówienie przechodzi na READY
11.3 Dlaczego to ważne

W sushi i na kuchni:

ekran szybko się brudzi,
dotykanie tabletu jest niewygodne,
fizyczne przyciski zmniejszają liczbę dotknięć.
12. Drukowanie

Drukowanie powinno być przesunięte na koniec procesu, nie na początek.

12.1 Nowy rekomendowany model

Nie drukujemy przy samym przyjęciu zamówienia.

Drukujemy dopiero:

po oznaczeniu zamówienia jako READY,
albo po stronie wydawki / kelnera.
12.2 Korzyści
mniej papieru,
mniej chaosu,
brak składowania toreb z bloczkami, zanim zamówienie jest gotowe,
lepszy moment na pakowanie.
12.3 Konfiguracja

Dodać ustawienie:

autoPrintOnReady = true|false

Opcjonalnie:

druk na konkretnej drukarce,
druk osobno dla wydawki,
druk osobno dla dostaw i odbiorów.
13. Komunikacja z backendem
    13.1 Start aplikacji

Po uruchomieniu:

pobierz aktywne zamówienia przez REST,
potem nasłuchuj zmian przez WebSocket / Socket.IO.
13.2 Podstawowe zdarzenia

Wystarczą:

ORDER_CREATED
ORDER_UPDATED
ORDER_READY
ORDER_CANCELLED

W trybie kolejkowym można zachować bardziej szczegółowe eventy, ale logika UI powinna umieć działać także w trybie uproszczonym.

13.3 Synchronizacja po reconnect

Po utracie połączenia:

po reconnect aplikacja pobiera ponownie aktywne zamówienia,
wyrównuje lokalny stan.

To jest ważniejsze niż poleganie wyłącznie na pojedynczych eventach.

14. Lokalny stan i architektura

Architektura MVVM + Compose zostaje, ale logika powinna być uproszczona.

14.1 Zamiast zbyt rozbudowanego flow

Dobrze, żeby ViewModel przechowywał:

listę aktywnych zamówień,
listę gotowych,
aktualny filtr,
tryb pracy,
aktualnie zaznaczone zamówienie,
stan połączenia,
błędy / loading.
14.2 Ważna uwaga

Jeśli tryb prosty jest główny, nie warto budować całego UI wokół 6 stanów.
Lepiej:

mieć prosty model prezentacji,
a dopiero backendowo mapować bardziej złożone stany, jeśli lokal tego potrzebuje.
15. Wydajność

To powinno być jednym z głównych założeń projektu.

15.1 Co ograniczyć
ciężkie animacje,
ciągłe pełne recomposition listy,
miganie kart,
sekundowe aktualizowanie wszystkiego na ekranie, jeśli nie trzeba.
15.2 Co zrobić
aktualizować tylko zmieniony bloczek,
ograniczyć dynamiczne przebudowywanie układu,
timer odświeżać oszczędnie,
używać stabilnych kluczy i stabilnych modeli UI,
preferować proste karty i czytelny kontrast.
15.3 Szczególnie ważne na 10"

Na słabszych tabletach nie można zakładać, że rozbudowany UI z wieloma stanami i ciągłym przeliczaniem będzie działał dobrze.

16. Ustawienia aplikacji

Do ustawień warto dodać:

16.1 Workflow
queueMode
autoPrintOnReady
showScheduledOrders
showProductionSummary
requireReadyConfirmation
16.2 Sterowanie
enableBluetoothKeys
mapowanie klawiszy:
next
previous
confirm
cancel
16.3 Terminal
kiosk mode
auto restart
reopen on background
17. Zalecane uproszczenia względem obecnej wersji

W obecnej dokumentacji warto ograniczyć lub zmienić:

17.1 Do uproszczenia
ACKED jako obowiązkowy krok
IN_PROGRESS jako obowiązkowy krok
akcje per item, jeśli kuchnia i tak pracuje głównie na całym zamówieniu
poziomy, które zwiększają liczbę kliknięć bez realnego zysku
17.2 Do zostawienia
REST + Socket.IO
DataStore
Hilt
Compose
Kiosk mode
reconnect i pełne odświeżenie po reconnect
auto-print
scheduled orders
priorytet rush
bezpieczeństwo i idempotency
17.3 Do dodania
tryb prosty jako główny
stabilny układ bloczków
wsparcie fizycznych przycisków
drukowanie po READY
agregacja podobnych zestawów
18. Docelowy efekt biznesowy

Po wdrożeniu system ma działać tak:

zamówienie wpada do POS / integracji
po akceptacji pojawia się na KDS
kuchnia robi zamówienie patrząc na ekran
po skończeniu klika GOTOWE
system:
aktualizuje status,
opcjonalnie drukuje bilecik,
informuje kelnera / wydawkę
kelner pakuje / wydaje gotowe zamówienie

Dzięki temu:

mniej papieru,
mniej chaosu,
mniej szukania bloczków,
większa kontrola nad gotowością zamówień,
prostsza praca dla kuchni.
19. Rekomendacja końcowa

Dla Waszego lokalu rekomendowany jest taki model:

domyślnie tryb prosty
drukowanie po READY
brak obowiązkowego ACK / IN_PROGRESS
stabilny grid zamiast przesuwającego się chaotycznie layoutu
obsługa 4 fizycznych przycisków
opcjonalne grupowanie podobnych zestawów jako osobny widok pomocniczy

Jeśli chcesz, mogę od razu zrobić Ci jeszcze wersję stricte dla Copilota / Cursora, czyli krótszą, techniczną specyfikację typu:

„przerób obecną aplikację KDS z tego workflow na uproszczony workflow ACTIVE/READY + stable grid + bluetooth keys + auto print on ready”.