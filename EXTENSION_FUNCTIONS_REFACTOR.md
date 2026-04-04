# 🔧 Skrypt Refaktoryzacji - Zastosowanie Extension Functions

## Cel
Zastąpienie powtarzającego się kodu `?: stringResource(R.string.common_dash)` 
na bardziej czytelne `.orDash()`

## Krok 1: Dodaj Import (raz w każdym pliku)

```kotlin
import com.itsorderchat.util.extensions.orDash
```

## Krok 2: Zastąp Kod (Find & Replace w Android Studio)

### Metoda 1: Global Find & Replace

1. W Android Studio: **Edit → Find → Replace in Files** (Ctrl+Shift+R)

2. **Find:**
```
?: stringResource(R.string.common_dash)
```

3. **Replace:**
```
.orDash()
```

4. **File mask:**
```
*.kt
```

5. Kliknij **Replace All** w `app/src/main/java/com/itsorderchat/ui/`

### Metoda 2: Plik po pliku

#### AcceptOrderSheetContent.kt (~20 miejsc)

**PRZED:**
```kotlin
value = order.consumer.phone ?: stringResource(R.string.common_dash)
value = order.consumer.email ?: stringResource(R.string.common_dash)
value = order.notes ?: stringResource(R.string.common_dash)
```

**PO:**
```kotlin
import com.itsorderchat.util.extensions.orDash

value = order.consumer.phone.orDash()
value = order.consumer.email.orDash()
value = order.notes.orDash()
```

## Inne Extension Functions do Zastosowania

### maskPhone() - Maskowanie numerów telefonu

**GDZIE:** Ekrany z danymi klienta (jeśli potrzebne RODO)

**PRZED:**
```kotlin
Text(customer.phone)
```

**PO:**
```kotlin
import com.itsorderchat.util.extensions.maskPhone
Text(customer.phone.maskPhone())
```

### isValidEmail() - Walidacja emaila

**GDZIE:** Formularze rejestracji/logowania

**PRZED:**
```kotlin
if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    // ...
}
```

**PO:**
```kotlin
import com.itsorderchat.util.extensions.isValidEmail
if (email.isValidEmail()) {
    // ...
}
```

### truncate() - Obcięcie długiego tekstu

**GDZIE:** Listy zamówień (długie notatki)

**PRZED:**
```kotlin
val shortNote = if (note.length > 50) "${note.take(47)}..." else note
```

**PO:**
```kotlin
import com.itsorderchat.util.extensions.truncate
val shortNote = note.truncate(50)
```

## Pliki Priorytetowe (w kolejności):

1. **AcceptOrderSheetContent.kt** (~20 wystąpień)
2. **OrderCard.kt** (~10 wystąpień) - jeśli istnieje
3. **OrderDetailScreen.kt** (~15 wystąpień) - jeśli istnieje
4. **HomeScreen.kt** (~5 wystąpień)
5. Inne pliki w `ui/`

## Automatyczne Narzędzie (opcjonalne)

Jeśli masz dużo plików, możesz użyć regex w IntelliJ:

**Find (Regex enabled):**
```regex
\?\s*:\s*stringResource\(R\.string\.common_dash\)
```

**Replace:**
```
.orDash()
```

## Sprawdzenie

Po zmianach:
1. Dodaj import w każdym pliku
2. Build → Rebuild Project
3. Sprawdź czy nie ma błędów kompilacji
4. Uruchom aplikację i przetestuj

## Szacowany Czas

- **Automatycznie (Find & Replace All):** 5 minut
- **Ręcznie (plik po pliku):** 30 minut
- **Testy:** 10 minut

**TOTAL:** 15-40 minut

