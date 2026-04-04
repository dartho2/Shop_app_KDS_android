# 🔬 Szczegółowa Diagnostyka Drukarki H10

## 📋 Status diagnostyki z Twojego urządzenia

### ✅ Potwierdzono (z logów):

1. **Pakiet `recieptservice.com.recieptservice`**
   - ✅ Istnieje i jest dostępny
   - ✅ Brak blokad uprawnień (`permission=null`)
   - ✅ Serwis jest exported (możemy się podłączyć)

2. **Pakiety alternatywne**
   - ❌ `com.senraise.printer` - NIE ISTNIEJE
   - ❌ `woyou.aidlservice.jiuiv5` - NIE ISTNIEJE
   - **Wniosek**: Urządzenie ma TYLKO klonowany sterownik

---

## 🎯 Dlaczego drukarka się łączy ale nic nie drukuje?

### Problem #1: Sterownik akceptuje wywołania, ale nie wykonuje druku

**Objaw:**
- `connect()` zwraca sukces
- `printText()` nie rzuca exception
- Ale drukarka nic nie drukuje (nawet feed)

**Przyczyna:**
Klonowane sterowniki `recieptservice` często mają:
- **Puste implementacje metod** - przyjmują wywołanie ale nic z nim nie robią
- **Wymagają specjalnego formatu** - np. grafiki zamiast tekstu
- **Potrzebują callbacka** - przekazywanie `null` może powodować brak reakcji

**Rozwiązanie:**
1. Sprawdź czy aplikacja vendorowa (jeśli jest) potrafi drukować
2. Poproś producenta o SDK lub przykładowy kod
3. Rozważ instalację jako system app (dostęp do `/dev/ttyS1`)

---

### Problem #2: Brak uprawnień do MCU (/dev/ttyS1)

**Objaw:**
- AIDL działa (bind sukces)
- Ale `nextLine()` (test FEED) nic nie robi

**Przyczyna:**
- Serwis `PrinterSender` wymaga uprawnień do portu szeregowego
- Tylko system apps mają dostęp do `/dev/ttyS1`

**Sprawdzenie:**
1. Uruchom **Test FEED** (📄)
2. Jeśli papier się NIE przesuwa → to ten problem

**Rozwiązanie:**
- Instalacja APK jako `/system/priv-app/` (wymaga root)
- LUB użyj aplikacji vendorowej jako pośrednika

---

## 🔧 Możliwe rozwiązania (w kolejności)

### 1️⃣ Znajdź aplikację vendorową (najprostsze)

**Co sprawdzić:**
```bash
# ADB shell
pm list packages | grep -i print
pm list packages | grep -i receipt
pm list packages | grep -i pos
```

**Jeśli znajdziesz** np. `com.example.printerapp`:
- Przetestuj czy ona drukuje
- Jeśli TAK → zbadaj jak wywołuje AIDL (dekompiluj APK)
- Skopiuj logikę do swojej aplikacji

---

### 2️⃣ Poproś producenta o SDK

**Informacje do podania:**
- Model: H10 / Senraise (klon)
- Pakiet sterownika: `recieptservice.com.recieptservice`
- Problem: AIDL bind działa, ale `printText()` nic nie drukuje

**Co chcesz otrzymać:**
- Bibliotekę `.aar` lub `.jar` z poprawną implementacją
- Przykładowy kod wywołania
- Dokumentację API

---

### 3️⃣ Bezpośredni dostęp do /dev/ttyS1 (wymaga root)

#### Krok 1: Sprawdź dostępność portu

```bash
# ADB shell (jako root)
su
ls -l /dev/ttyS*
```

**Spodziewany wynik:**
```
crw-rw---- 1 system system 204, 64 ... /dev/ttyS1
```

#### Krok 2: Zainstaluj aplikację jako system app

**Metoda A: ADB push (wymaga root + remount)**
```bash
adb root
adb remount
adb push app-debug.apk /system/priv-app/ItsOrderChat/ItsOrderChat.apk
adb shell chmod 644 /system/priv-app/ItsOrderChat/ItsOrderChat.apk
adb reboot
```

**Metoda B: Modyfikacja obrazu systemu**
- Wymaga dostępu do firmware
- Dodanie APK do `/system/priv-app/`
- Rebuild image

#### Krok 3: Nadaj uprawnienia

Utwórz plik `/system/etc/permissions/privapp-permissions-itsorderchat.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.itsorderchat">
        <permission name="android.permission.WRITE_SECURE_SETTINGS"/>
        <permission name="android.permission.WRITE_MEDIA_STORAGE"/>
    </privapp-permissions>
</permissions>
```

#### Krok 4: Drukuj bezpośrednio (kod)

Po instalacji jako system app możesz otworzyć port:

```kotlin
import android_serialport_api.SerialPort
import java.io.File

fun printDirectly(text: String) {
    val port = SerialPort(File("/dev/ttyS1"), 115200, 0)
    port.outputStream.use { out ->
        // ESC @ (init)
        out.write(byteArrayOf(0x1B, 0x40))
        // Tekst
        out.write(text.toByteArray(Charsets.UTF_8))
        // Feed
        out.write("\n\n\n".toByteArray())
        out.flush()
    }
    port.close()
}
```

**Zalety:**
- Pełna kontrola nad drukarką
- Wysyłanie czystych komend ESC/POS
- Brak zależności od buggy driverów

**Wady:**
- Wymaga roota
- Może złamać gwarancję
- Trzeba znać protokół ESC/POS

---

### 4️⃣ Helper App (kompromis)

**Idea:**
- Stwórz minimalną aplikację jako `/system/priv-app/` (podpisaną kluczem platform)
- Ona ma dostęp do `/dev/ttyS1`
- Twoja główna aplikacja komunikuje się z helperem przez AIDL lub broadcast

**Zalety:**
- Główna apka nie wymaga root
- Helper można zainstalować raz i używać z wielu aplikacji

**Wady:**
- Wymaga współpracy z producentem (podpis platform key)
- Dodatkowa złożoność

---

## 🧪 Checklist diagnostyczny

Wykonaj kolejno i zaznacz:

- [ ] **Test 1**: Diagnostyka systemowa (🔒) pokazuje `recieptservice` bez blokad
- [ ] **Test 2**: Test FEED (📄) - czy papier się przesuwa?
  - [ ] ✅ TAK → Problem w formacie danych
  - [ ] ❌ NIE → Problem w uprawnieniach MCU
- [ ] **Test 3**: Czy istnieje aplikacja vendorowa?
  - [ ] ✅ TAK i drukuje → Zbadaj jak wywołuje AIDL
  - [ ] ❌ NIE lub nie drukuje → Kontakt z producentem
- [ ] **Test 4**: Czy mam dostęp do root?
  - [ ] ✅ TAK → Rozważ `/system/priv-app/`
  - [ ] ❌ NIE → Muszę użyć AIDL (SDK od producenta)

---

## 📞 Kontakt z producentem

**Szablon emaila:**

```
Temat: SDK request for H10 POS terminal printer integration

Witam,

Rozwijam aplikację POS dla terminala H10 i potrzebuję pomocy z integracją 
wbudowanej drukarki termicznej.

Terminal:
- Model: H10 / Senraise (klon)
- Pakiet sterownika: recieptservice.com.recieptservice
- Android: [wersja]

Problem:
- AIDL bind do PrinterService działa poprawnie (brak exception)
- printText() i nextLine() nie rzucają wyjątków
- ALE drukarka nie drukuje (brak reakcji fizycznej)

Proszę o:
1. SDK/bibliotekę do integracji z drukarką
2. Przykładowy kod wywołania
3. Dokumentację API
4. Informację czy wymagane są specjalne uprawnienia

Dziękuję!
```

---

## 🎯 Podsumowanie dla Twojego przypadku

**Najbardziej prawdopodobny scenariusz:**
1. ✅ Sterownik `recieptservice` jest dostępny
2. ❌ Nie ma uprawnień do MCU lub wymaga specjalnego formatu

**Najlepsza droga naprzód:**
1. **Najpierw**: Sprawdź Test FEED - to powie czy problem jest w AIDL czy MCU
2. **Jeśli FEED działa**: Zbadaj aplikację vendorową lub poproś o SDK
3. **Jeśli FEED NIE działa**: Rozważ system app lub kontakt z producentem

**Czasami trzeba pogodzić się z tym, że:**
- Klonowane terminale mają buggy/niepełne sterowniki
- Jedyne rozwiązanie to dostęp do `/dev/ttyS1` (system app)
- LUB użycie aplikacji vendorowej jako pośrednika

---

**Powodzenia! 🚀**

Jeśli nic nie zadziała, rozważ zakup terminala od renomowanego producenta 
(Sunmi, PAX, Newland) - mają profesjonalne SDK i wsparcie.

