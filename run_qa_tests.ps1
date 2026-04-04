# 🧪 ETAP 5: AUTOMATED QA TEST SCRIPT (PowerShell)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🧪 ETAP 5: AUTOMATED QA TESTS" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$TESTS_PASSED = 0
$TESTS_FAILED = 0

# Funkcja testowa
function Run-Test {
    param(
        [string]$TestName,
        [scriptblock]$TestCmd
    )

    Write-Host -NoNewline "Testing: $TestName... "

    try {
        $result = & $TestCmd
        if ($result) {
            Write-Host "✅ PASS" -ForegroundColor Green
            $script:TESTS_PASSED++
        } else {
            Write-Host "❌ FAIL" -ForegroundColor Red
            $script:TESTS_FAILED++
        }
    } catch {
        Write-Host "❌ FAIL" -ForegroundColor Red
        $script:TESTS_FAILED++
    }
}

Write-Host "🏗️ TEST 1: Build Verification" -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

# Test 1: Printer.kt istnieje
Run-Test "Printer.kt Exists" {
    Test-Path "L:\SHOP APP\app\src\main\java\com\itsorderchat\data\model\Printer.kt"
}

# Test 2: PrinterProfile.kt istnieje
Run-Test "PrinterProfile.kt Exists" {
    Test-Path "L:\SHOP APP\app\src\main\java\com\itsorderchat\data\model\PrinterProfile.kt"
}

# Test 3: PrintersViewModel.kt istnieje
Run-Test "PrintersViewModel.kt Exists" {
    Test-Path "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\printer\PrintersViewModel.kt"
}

# Test 4: PrintersListScreen.kt istnieje
Run-Test "PrintersListScreen.kt Exists" {
    Test-Path "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\printer\PrintersListScreen.kt"
}

# Test 5: AddEditPrinterDialog.kt istnieje
Run-Test "AddEditPrinterDialog.kt Exists" {
    Test-Path "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\printer\AddEditPrinterDialog.kt"
}

Write-Host ""
Write-Host "📋 TEST 2: Code Content Validation" -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

# Test 6: Printer.kt zawiera data class
Run-Test "Printer.kt has data class" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\data\model\Printer.kt") -match "data class Printer"
}

# Test 7: PrinterProfile.kt zawiera enum
Run-Test "PrinterProfile.kt has enum" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\data\model\PrinterProfile.kt") -match "enum class PrinterProfile"
}

# Test 8: PrintersViewModel zawiera @HiltViewModel
Run-Test "PrintersViewModel has @HiltViewModel" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\printer\PrintersViewModel.kt") -match "@HiltViewModel"
}

# Test 9: PrintersListScreen zawiera @Composable
Run-Test "PrintersListScreen has @Composable" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\printer\PrintersListScreen.kt") -match "@Composable"
}

Write-Host ""
Write-Host "📋 TEST 3: Integration Points" -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

# Test 10: HomeActivity zawiera callback
Run-Test "HomeActivity has onNavigateToPrintersList" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\theme\home\HomeActivity.kt") -match "onNavigateToPrintersList"
}

# Test 11: SettingsScreen zawiera callback
Run-Test "SettingsScreen has onNavigateToPrintersList" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\SettingsScreen.kt") -match "onNavigateToPrintersList"
}

# Test 12: PrinterService zawiera nową metodę
Run-Test "PrinterService has printOrderOnAllEnabledPrinters" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\PrinterService.kt") -match "printOrderOnAllEnabledPrinters"
}

# Test 13: AppDestinations zawiera PRINTERS_LIST
Run-Test "HomeActivity AppDestinations has PRINTERS_LIST" {
    (Get-Content "L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\theme\home\HomeActivity.kt") -match "PRINTERS_LIST"
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "📊 TEST RESULTS" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "✅ Passed: $TESTS_PASSED" -ForegroundColor Green
Write-Host "❌ Failed: $TESTS_FAILED" -ForegroundColor Red
$TOTAL = $TESTS_PASSED + $TESTS_FAILED
Write-Host "📈 Total: $TOTAL" -ForegroundColor Cyan
Write-Host ""

if ($TESTS_FAILED -eq 0) {
    Write-Host "🎉 ALL TESTS PASSED!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️ SOME TESTS FAILED - CHECK ABOVE" -ForegroundColor Red
    exit 1
}

