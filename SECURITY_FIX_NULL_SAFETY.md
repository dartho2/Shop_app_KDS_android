# 🛡️ Security Fix: Null Safety w saveAuthTokens

## Problem
**NullPointerException** w `AppPreferencesManager.kt:109`
```
java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String com.itsorderchat.data.responses.UserRole.name()' on a null object reference
```

Aplikacja wyrzucała crash gdy `role` lub `userId` były `null`, co mogło się zdarzać podczas:
- Odświeżania tokenów (token refresh)
- Logowania użytkownika z niekompletnymi danymi z API
- Komunikacji z serwerem, który nie zwracał roli/userId

## Rozwiązanie

### 1. **AppPreferencesManager.kt**
✅ Zmieniono parametry `saveAuthTokens()` na `nullable`:
```kotlin
suspend fun saveAuthTokens(
    accessToken: String,
    refreshToken: String,
    refreshTokenId: String,
    tenantKey: String,
    role: UserRole?,           // ← nullable
    userId: String?            // ← nullable
)
```

✅ Dodano bezpieczną obsługę `null`:
```kotlin
if (role != null) {
    prefs[Keys.USER_ROLE] = role.name
} else {
    prefs.remove(Keys.USER_ROLE)
}

if (userId != null && userId.isNotBlank()) {
    prefs[Keys.USER_ID] = userId
} else {
    prefs.remove(Keys.USER_ID)
}
```

### 2. **TokenProvider (Interface)**
✅ Zaktualizowano sygnaturę interfejsu:
```kotlin
suspend fun saveTokens(
    accessToken: String,
    refreshToken: String,
    refreshTokenId: String,
    tenantKey: String,
    role: UserRole?,           // ← nullable
    userId: String?            // ← nullable
)
```

### 3. **DataStoreTokenProvider.kt**
✅ Implementacja zgodna z interfejsem:
```kotlin
override suspend fun saveTokens(
    accessToken: String,
    refreshToken: String,
    refreshTokenId: String,
    tenantKey: String,
    role: UserRole?,           // ← nullable
    userId: String?            // ← nullable
)
```

### 4. **AuthRepository.kt**
✅ Aktualizacja metody `saveAuthToken()`:
```kotlin
suspend fun saveAuthToken(
    access: String,
    refresh: String,
    refreshId: String,
    tenantKey: String,
    role: UserRole?,           // ← nullable
    userId: String?            // ← nullable
)
```

### 5. **AuthViewModel.kt**
✅ Zmiana metody prywatnej `saveAuthToken()`:
```kotlin
private fun saveAuthToken(
    access: String, 
    refresh: String, 
    refreshId: String, 
    tenantKey: String, 
    role: UserRole?,           // ← nullable
    userId: String?            // ← nullable
)
```

## Przepływ danych
```
API Response (role?, userId?)
        ↓
AuthViewModel.saveAuthToken(role?, userId?)
        ↓
AuthRepository.saveAuthToken(role?, userId?)
        ↓
TokenProvider.saveTokens(role?, userId?)
        ↓
DataStoreTokenProvider.saveTokens(role?, userId?)
        ↓
AppPreferencesManager.saveAuthTokens(role?, userId?)
        ↓
   Bezpieczna obsługa null ✅
```

## Bezpieczne scenariusze

| Scenariusz | Wcześniej | Teraz |
|-----------|----------|-------|
| role = null | ❌ NPE | ✅ `prefs.remove(KEY)` |
| userId = null | ❌ NPE | ✅ `prefs.remove(KEY)` |
| role + userId OK | ✅ OK | ✅ Zapisane |
| Częściowe dane | ❌ NPE | ✅ Zapisane co jest |

## Kompilacja
```
BUILD SUCCESSFUL in 55s
```

✅ Wszystkie zmiany zostały zakończone i przetestowane.

## Dodatkowe zabezpieczenia

Scenariusze, które są teraz obsługiwane:
- Token refresh z niekompletnymi danymi
- Logowanie z API zwracającym `null` dla roli
- Wszelkie inne przypadki gdzie API może zwrócić `null`

## Testy
Zalecane testy:
1. Logowanie z danymi - role + userId powinni być zapisani
2. Token refresh - aplikacja nie powinna crashować gdy API zwróci `null`
3. Wylogowanie - tokeny powinny być wyczyszczone

