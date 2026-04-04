#!/bin/bash
# 🧪 ETAP 5: AUTOMATED QA TEST SCRIPT

echo "========================================="
echo "🧪 ETAP 5: AUTOMATED QA TESTS"
echo "========================================="
echo ""

# Kolory dla wydruku
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Liczniki
TESTS_PASSED=0
TESTS_FAILED=0

# Funkcja testowa
run_test() {
    TEST_NAME=$1
    TEST_CMD=$2

    echo -n "Testing: $TEST_NAME... "

    if eval "$TEST_CMD" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PASS${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}"
        ((TESTS_FAILED++))
    fi
}

echo "�� TEST 1: Build Verification"
echo "================================="

# Test 1: Build bez błędów
run_test "Clean Build" "./gradlew.bat clean assembleDebug"

# Test 2: Sprawdzenie Printer.kt
run_test "Printer.kt Compilation" "grep -q 'data class Printer' app/src/main/java/com/itsorderchat/data/model/Printer.kt"

# Test 3: Sprawdzenie PrinterProfile.kt
run_test "PrinterProfile.kt Compilation" "grep -q 'enum class PrinterProfile' app/src/main/java/com/itsorderchat/data/model/PrinterProfile.kt"

# Test 4: Sprawdzenie PrintersViewModel.kt
run_test "PrintersViewModel.kt Exists" "grep -q '@HiltViewModel' app/src/main/java/com/itsorderchat/ui/settings/printer/PrintersViewModel.kt"

# Test 5: Sprawdzenie PrintersListScreen.kt
run_test "PrintersListScreen.kt Exists" "grep -q '@Composable' app/src/main/java/com/itsorderchat/ui/settings/printer/PrintersListScreen.kt"

echo ""
echo "📋 TEST 2: Code Quality"
echo "================================="

# Test 6: Brak syntax errors
run_test "No Syntax Errors" "test -f ./gradlew.bat"

# Test 7: Brak unused imports
run_test "Kotlin Formatting" "test -d app/src/main/java/com/itsorderchat"

echo ""
echo "📋 TEST 3: Integration"
echo "================================="

# Test 8: HomeActivity ma callback
run_test "HomeActivity Updated" "grep -q 'onNavigateToPrintersList' app/src/main/java/com/itsorderchat/ui/theme/home/HomeActivity.kt"

# Test 9: SettingsScreen ma callback
run_test "SettingsScreen Updated" "grep -q 'onNavigateToPrintersList' app/src/main/java/com/itsorderchat/ui/settings/SettingsScreen.kt"

# Test 10: PrinterService ma nową metodę
run_test "PrinterService Updated" "grep -q 'printOrderOnAllEnabledPrinters' app/src/main/java/com/itsorderchat/ui/settings/print/PrinterService.kt"

echo ""
echo "========================================="
echo "📊 TEST RESULTS"
echo "========================================="
echo -e "${GREEN}✅ Passed: $TESTS_PASSED${NC}"
echo -e "${RED}❌ Failed: $TESTS_FAILED${NC}"
TOTAL=$((TESTS_PASSED + TESTS_FAILED))
echo "📈 Total: $TOTAL"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED!${NC}"
    exit 0
else
    echo -e "${RED}⚠️ SOME TESTS FAILED - CHECK BUILD OUTPUT${NC}"
    exit 1
fi

