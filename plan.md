🚀 Kroki implementacji
1. 🔌 Rozszerzenie SocketEvent.kt

Dodać eventy KDS:

KDS_TICKET_CREATED
KDS_TICKET_STARTED
KDS_TICKET_ACKED
KDS_TICKET_READY
KDS_TICKET_HANDED_OFF
KDS_TICKET_CANCELLED
KDS_ITEM_STARTED
KDS_ITEM_READY
2. 📡 KdsSocketEventsRepository.kt

Lokalizacja: service/

Opis:

analogiczny do SocketEventsRepository.kt
przechowuje dane z socketów

Struktura:

MutableSharedFlow<KdsTicket> → nowe / zaktualizowane tickety
MutableSharedFlow<String> → tickety zakończone (ticketId)
3. 🧩 KdsSocketEventsHandler.kt

Lokalizacja: service/
Wzorowany na: SocketStaffEventsHandler.kt

Kluczowe elementy:
eventHandlers → mapa eventów KDS
Obsługa eventów:

KDS_TICKET_CREATED

pobierz pełny ticket:
KdsRepository.getTicketWithItems(ticketId)
emituj do repository

Pozostałe eventy

emituj:
KdsTicketStateEvent(ticketId, newState)
4. 🌐 API — NetworkModule.kt

Dodać:

@Provides
fun provideKdsApi(): KdsApi
analogicznie do provideOrderApi()
użyć:
@Named("auth_retrofit")

📍 Umieścić przed zamknięciem NetworkModule

5. 🧠 KdsViewModel.kt

Lokalizacja: ui/kds/
Adnotacja: @HiltViewModel

Stan:
MutableStateFlow<Map<String, KdsTicketWithItems>>
Logika:

init {}

pobranie ticketów:
KdsRepository.getTickets("NEW,ACKED,IN_PROGRESS,READY")

Real-time updates

collectLatest z KdsSocketEventsRepository
aktualizacja mapy
Metody:
ackTicket()
startTicket()
readyTicket()
handoffTicket()

➡️ Każda:

wywołuje KdsRepository
aktualizuje lokalny stan po sukcesie
6. 🎨 UI — KdsTicketCard.kt + KdsScreen.kt

Lokalizacja: ui/kds/

🧾 KdsTicketCard

Zawiera:

orderNumber
priorytet (RUSH jako Chip)
timer SLA (kolor czerwony, gdy < 0)
lista KdsTicketItem
przyciski:
Status	Akcja
NEW	ACK
ACKED	START
IN_PROGRESS	READY
READY	HANDOFF
📺 KdsScreen
LazyRow ticketów
FilterChip dla statusów:
NEW
ACKED
IN_PROGRESS
READY
7. 🔄 Integracja z aplikacją
   HomeActivity.kt
   dodać:
   val kdsViewModel by viewModels<KdsViewModel>()
   podmienić:
   HomeScreen → KdsScreen
   SocketService

Zarejestrować:

kdsSocketEventsHandler.register()

➡️ razem z:

SocketStaffEventsHandler.register()
🤔 Dalsze rozważania
🔹 1. Skąd ticketId w socketach?

Jeśli payload zawiera tylko:

{ "ticketId": "..." }

➡️ Handler:

parsuje JSON
emituje:
KdsTicketStateEvent(ticketId, state)
🔹 2. Timer SLA
Opcja A — w UI (prostsza)
LaunchedEffect
delay(1000)

❌ minus:

każda karta działa osobno
Opcja B — w ViewModel (lepsza)
jeden ticker globalny
większa wydajność
🔹 3. Filtrowanie po stationId

Pytanie:

czy MVP uwzględnia stanowiska?

Opcje:

❌ NIE → pokazujemy wszystkie tickety
✅ TAK → filtrujemy po KdsStation
🧩 Podsumowanie

System KDS opiera się na:

StateFlow + Map ticketów
Socket.IO + HTTP
Reactive UI (Compose)

Cel:
👉 szybki, czytelny i stabilny system dla kuchni 🚀