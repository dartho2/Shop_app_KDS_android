# Skrypt weryfikacji struktury projektu po przeniesieniu folderów
# Użycie: .\verify-structure.ps1

Write-Host "=== Weryfikacja Struktury Projektu ===" -ForegroundColor Cyan
Write-Host ""

$projectRoot = "L:\SHOP APP"
$errorsFound = $false

# Sprawdź czy foldery z literówkami nadal istnieją
Write-Host "1. Sprawdzanie literówek w nazwach folderów..." -ForegroundColor Yellow

$badFolders = @(
    "app\src\main\java\com\itsorderchat\ui\utili",
    "app\src\main\java\com\itsorderchat\data\entity\datebase"
)

foreach ($folder in $badFolders) {
    $fullPath = Join-Path $projectRoot $folder
    if (Test-Path $fullPath) {
        Write-Host "   ❌ ZNALEZIONO LITERÓWKĘ: $folder" -ForegroundColor Red
        $errorsFound = $true
    } else {
        Write-Host "   ✅ Literówka nie istnieje: $folder (dobrze!)" -ForegroundColor Green
    }
}

Write-Host ""

# Sprawdź czy poprawne foldery istnieją
Write-Host "2. Sprawdzanie poprawnych folderów..." -ForegroundColor Yellow

$goodFolders = @(
    "app\src\main\java\com\itsorderchat\ui\util",
    "app\src\main\java\com\itsorderchat\data\entity\database"
)

foreach ($folder in $goodFolders) {
    $fullPath = Join-Path $projectRoot $folder
    if (Test-Path $fullPath) {
        Write-Host "   ✅ Poprawny folder istnieje: $folder" -ForegroundColor Green
    } else {
        Write-Host "   ❌ BRAK POPRAWNEGO FOLDERU: $folder" -ForegroundColor Red
        $errorsFound = $true
    }
}

Write-Host ""

# Sprawdź pliki w poprawnych folderach
Write-Host "3. Sprawdzanie zawartości folderów..." -ForegroundColor Yellow

$utilPath = Join-Path $projectRoot "app\src\main\java\com\itsorderchat\ui\util"
if (Test-Path $utilPath) {
    $utilFiles = Get-ChildItem $utilPath -Filter "*.kt"
    Write-Host "   📁 ui/util/ zawiera $($utilFiles.Count) plików:" -ForegroundColor Cyan
    foreach ($file in $utilFiles) {
        Write-Host "      - $($file.Name)" -ForegroundColor Gray
    }
} else {
    Write-Host "   ⚠️  Folder ui/util/ nie istnieje" -ForegroundColor Yellow
}

Write-Host ""

$dbPath = Join-Path $projectRoot "app\src\main\java\com\itsorderchat\data\entity\database"
if (Test-Path $dbPath) {
    $dbFiles = Get-ChildItem $dbPath -Filter "*.kt"
    Write-Host "   📁 data/entity/database/ zawiera $($dbFiles.Count) plików:" -ForegroundColor Cyan
    foreach ($file in $dbFiles) {
        Write-Host "      - $($file.Name)" -ForegroundColor Gray
    }
} else {
    Write-Host "   ⚠️  Folder data/entity/database/ nie istnieje" -ForegroundColor Yellow
}

Write-Host ""

# Sprawdź deprecated pliki
Write-Host "4. Sprawdzanie deprecated plików (do usunięcia)..." -ForegroundColor Yellow

$deprecatedFiles = @(
    "app\src\main\java\com\itsorderchat\ui\product\ProductsRepository.kt",
    "app\src\main\java\com\itsorderchat\ui\product\ProductApi.kt",
    "app\src\main\java\com\itsorderchat\ui\settings\SettingsRepository.kt",
    "app\src\main\java\com\itsorderchat\ui\vehicle\VehicleRepository.kt",
    "app\src\main\java\com\itsorderchat\ui\vehicle\VehicleApi.kt"
)

$deprecatedCount = 0
foreach ($file in $deprecatedFiles) {
    $fullPath = Join-Path $projectRoot $file
    if (Test-Path $fullPath) {
        Write-Host "   ⚠️  Deprecated plik nadal istnieje: $file" -ForegroundColor Yellow
        $deprecatedCount++
    }
}

if ($deprecatedCount -eq 0) {
    Write-Host "   ✅ Wszystkie deprecated pliki usunięte" -ForegroundColor Green
} else {
    Write-Host "   📝 Znaleziono $deprecatedCount deprecated plików do usunięcia" -ForegroundColor Yellow
}

Write-Host ""

# Sprawdź nowe pliki
Write-Host "5. Sprawdzanie nowych plików (przeniesione)..." -ForegroundColor Yellow

$newFiles = @(
    "app\src\main\java\com\itsorderchat\data\repository\ProductsRepository.kt",
    "app\src\main\java\com\itsorderchat\data\repository\SettingsRepository.kt",
    "app\src\main\java\com\itsorderchat\data\repository\VehicleRepository.kt",
    "app\src\main\java\com\itsorderchat\data\network\ProductApi.kt",
    "app\src\main\java\com\itsorderchat\data\network\VehicleApi.kt"
)

$newFilesCount = 0
foreach ($file in $newFiles) {
    $fullPath = Join-Path $projectRoot $file
    if (Test-Path $fullPath) {
        Write-Host "   ✅ Nowy plik istnieje: $file" -ForegroundColor Green
        $newFilesCount++
    } else {
        Write-Host "   ❌ BRAK nowego pliku: $file" -ForegroundColor Red
        $errorsFound = $true
    }
}

Write-Host ""

# Podsumowanie
Write-Host "=== PODSUMOWANIE ===" -ForegroundColor Cyan
Write-Host ""

if ($errorsFound) {
    Write-Host "❌ Znaleziono problemy!" -ForegroundColor Red
    Write-Host "   Sprawdź powyższe błędy i napraw je." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Jeśli nie przeniosłeś jeszcze folderów:" -ForegroundColor Yellow
    Write-Host "   → Zobacz: FOLDER_RENAME_INSTRUCTIONS.md" -ForegroundColor Cyan
} else {
    Write-Host "✅ Struktura wygląda dobrze!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Następne kroki:" -ForegroundColor Cyan
    Write-Host "   1. Build → Clean Project" -ForegroundColor White
    Write-Host "   2. Build → Rebuild Project" -ForegroundColor White
    Write-Host "   3. Uruchom aplikację i przetestuj" -ForegroundColor White

    if ($deprecatedCount -gt 0) {
        Write-Host ""
        Write-Host "Opcjonalnie: Usuń $deprecatedCount deprecated plików" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Koniec weryfikacji ===" -ForegroundColor Cyan

