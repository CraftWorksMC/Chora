package com.craftworks.music.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Colors (Deep Blue/Slate)
val Slate80 = Color(0xFFBCC6E0)
val SlateGrey80 = Color(0xFFC4C7D0)
val SoftRed80 = Color(0xFFE0BCBC)

val Slate40 = Color(0xFF3F517D)
val SlateGrey40 = Color(0xFF575E71)
val SoftRed40 = Color(0xFF7D5252)

// Keeping the original names for compatibility if needed, but pointing to new colors is cleaner.
// For now, I will replace the variable definitions to minimize changes in Theme.kt
// But wait, Theme.kt uses Purple80 etc. I should update Theme.kt to use the new names or alias them.
// I'll update Theme.kt to use semantic names if possible, but for minimal impact, I'll just redefine the values
// and rename the variables in Theme.kt to match.

// Actually, let's keep it clean.
val PrimaryLight = Color(0xFF415f91)
val OnPrimaryLight = Color(0xFFffffff)
val PrimaryContainerLight = Color(0xFFd6e3ff)
val OnPrimaryContainerLight = Color(0xFF001b3e)

val SecondaryLight = Color(0xFF565f71)
val OnSecondaryLight = Color(0xFFffffff)
val SecondaryContainerLight = Color(0xFFdae2f9)
val OnSecondaryContainerLight = Color(0xFF131c2b)

val TertiaryLight = Color(0xFF705575)
val OnTertiaryLight = Color(0xFFffffff)
val TertiaryContainerLight = Color(0xFFfad8fd)
val OnTertiaryContainerLight = Color(0xFF28132e)

val ErrorLight = Color(0xFFba1a1a)
val OnErrorLight = Color(0xFFffffff)
val ErrorContainerLight = Color(0xFFffdad6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFf9f9ff)
val OnBackgroundLight = Color(0xFF191c20)
val SurfaceLight = Color(0xFFf9f9ff)
val OnSurfaceLight = Color(0xFF191c20)

// Dark Scheme
val PrimaryDark = Color(0xFFaac7ff)
val OnPrimaryDark = Color(0xFF0a305f)
val PrimaryContainerDark = Color(0xFF284777)
val OnPrimaryContainerDark = Color(0xFFd6e3ff)

val SecondaryDark = Color(0xFFbec6dc)
val OnSecondaryDark = Color(0xFF283141)
val SecondaryContainerDark = Color(0xFF3e4759)
val OnSecondaryContainerDark = Color(0xFFdae2f9)

val TertiaryDark = Color(0xFFddbce0)
val OnTertiaryDark = Color(0xFF3f2844)
val TertiaryContainerDark = Color(0xFF573e5c)
val OnTertiaryContainerDark = Color(0xFFfad8fd)

val ErrorDark = Color(0xFFffb4ab)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000a)
val OnErrorContainerDark = Color(0xFFffdad6)

val BackgroundDark = Color(0xFF111318)
val OnBackgroundDark = Color(0xFFe2e2e9)
val SurfaceDark = Color(0xFF111318)
val OnSurfaceDark = Color(0xFFe2e2e9)
