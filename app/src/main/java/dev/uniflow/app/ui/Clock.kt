package dev.uniflow.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalDateTime

val LocalNow = staticCompositionLocalOf { LocalDateTime.now() }
