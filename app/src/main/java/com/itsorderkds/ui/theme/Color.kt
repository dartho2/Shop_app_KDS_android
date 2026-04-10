package com.itsorderkds.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Legacy (nieusuwane — mogą być używane w innych miejscach) ───────────────
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// ─────────────────────────────────────────────────────────────────────────────
// KDS PROFESSIONAL DARK THEME
// Filozofia:
//   • Tło maksymalnie ciemne — oczy kucharza nie męczą się przy długiej pracy
//   • Karty lekko jaśniejsze — wyraźnie oddzielone od tła, ale nie białe
//   • Tekst wysokokontrastowy — czytelny z odległości
//   • Akcenty minimalne — tylko to co ważne przyciąga uwagę
//   • SLA kolory zachowują semantykę (zielony OK, żółty uwaga, czerwony alarm)
// ─────────────────────────────────────────────────────────────────────────────

// ── Tło aplikacji — prawie czarne, z delikatnym odcieniem chłodnego szarego ──
val KdsBg            = Color(0xFF0D0F12)   // ekran główny KDS
val KdsBgElevated    = Color(0xFF13171D)   // Scaffold / drawer background

// ── Karty ticketów — ciemny węgiel, wyraźnie widoczne na tle ─────────────────
val KdsCard          = Color(0xFF1C2129)   // domyślna karta
val KdsCardAlt       = Color(0xFF1A1F26)   // lekko alternatywna (hover/focus)
val KdsCardBorder    = Color(0xFF2C3340)   // obramowanie karty (subtelne)

// ── Tekst ─────────────────────────────────────────────────────────────────────
val KdsTextPrimary   = Color(0xFFF0F2F5)   // główny tekst — niemal biały, bez zmęczenia oczu
val KdsTextSecondary = Color(0xFF8B95A1)   // drugorzędny — numer zamówienia, czas
val KdsTextMuted     = Color(0xFF4B5563)   // wygaszone (stany DONE/VOID)

// ── Akcent — brand pomarańczowy (używany minimalnie) ─────────────────────────
val KdsAccent        = Color(0xFFFF8C42)   // ciepły pomarańcz — przyciski główne
val KdsAccentMuted   = Color(0xFF3D2010)   // tło akcentu (chip, badge)

// ── Kolory semantyczne SLA (zoptymalizowane pod dark) ────────────────────────
val KdsSlaGreen      = Color(0xFF4CAF50)   // > 5 min — spokojnie
val KdsSlaYellow     = Color(0xFFFFCA28)   // 0–5 min — uwaga
val KdsSlaRed        = Color(0xFFF44336)   // minął czas — alarm
val KdsSlaGray       = Color(0xFF546E7A)   // stan wygasły / DONE
val KdsSlaBlue       = Color(0xFF42A5F5)   // WYDANO / HANDED_OFF

// ── Kolor zamówień zaplanowanych ─────────────────────────────────────────────
val KdsScheduled     = Color(0xFFBA68C8)   // fiolet — zaplanowane w przyszłości

// ── Divider / outline ─────────────────────────────────────────────────────────
val KdsDivider       = Color(0xFF252B34)
val KdsOutline       = Color(0xFF353D4A)

// ─────────────────────────────────────────────────────────────────────────────
// LIGHT scheme (zachowane dla ustawień / loginu jeśli potrzebne)
// ─────────────────────────────────────────────────────────────────────────────
val PrimaryLight            = Color(0xFFF97316)
val OnPrimaryLight          = Color(0xFFFFFFFF)
val PrimaryContainerLight   = Color(0xFFFFE9D7)
val OnPrimaryContainerLight = Color(0xFF5A2500)
val SecondaryLight          = Color(0xFF0F172A)
val OnSecondaryLight        = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE2E8F0)
val OnSecondaryContainerLight = Color(0xFF0F172A)
val BackgroundLight         = Color(0xFFF9FAFB)
val OnBackgroundLight       = Color(0xFF111827)
val SurfaceLight            = Color(0xFFFFFFFF)
val OnSurfaceLight          = Color(0xFF111827)
val SurfaceVariantLight     = Color(0xFFE5E7EB)
val OnSurfaceVariantLight   = Color(0xFF44474F)
val OutlineVariantLight     = Color(0xFFC7CED6)
val ErrorLight              = Color(0xFFD32F2F)
val OnErrorLight            = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// DARK scheme tokeny (używane w Theme.kt)
// ─────────────────────────────────────────────────────────────────────────────
val PrimaryDark             = KdsAccent
val OnPrimaryDark           = Color(0xFF1A0A00)
val PrimaryContainerDark    = KdsAccentMuted
val OnPrimaryContainerDark  = Color(0xFFFFDCC5)
val SecondaryDark           = Color(0xFF90A4AE)
val OnSecondaryDark         = Color(0xFF0A0F1A)
val SecondaryContainerDark  = KdsCard
val OnSecondaryContainerDark = KdsTextPrimary
val BackgroundDark          = KdsBg
val OnBackgroundDark        = KdsTextPrimary
val SurfaceDark             = KdsBgElevated
val OnSurfaceDark           = KdsTextPrimary
val SurfaceVariantDark      = KdsCard
val OnSurfaceVariantDark    = KdsTextSecondary
val ErrorDark               = Color(0xFFEF5350)
val OnErrorDark             = Color(0xFF410002)

// ── SUCCESS ───────────────────────────────────────────────────────────────────
val SuccessLight            = Color(0xFF2E7D32)
val OnSuccessLight          = Color(0xFFFFFFFF)
val SuccessDark             = KdsSlaGreen
val OnSuccessDark           = Color(0xFF00210A)
val SuccessContainerLight   = Color(0xFFE8F5E9)
val OnSuccessContainerLight = Color(0xFF0B3D0B)
val SuccessContainerDark    = Color(0xFF0B3D0B)
val OnSuccessContainerDark  = Color(0xFFE8F5E9)
