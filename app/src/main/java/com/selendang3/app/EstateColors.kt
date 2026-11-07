package com.selendang3.app

// Compatibility facade for screens not yet migrated to core.theme.
// Remove after the remaining UI screens import the theme package directly.
@Deprecated("Import from core.theme")
typealias BookAccent = com.selendang3.app.core.theme.BookAccent

val BookSlate get() = com.selendang3.app.core.theme.BookSlate
val BookTeal get() = com.selendang3.app.core.theme.BookTeal
val BookInk get() = com.selendang3.app.core.theme.BookInk

fun bookAccent(key: String) = com.selendang3.app.core.theme.bookAccent(key)

fun estateColorScheme(dark: Boolean, accent: BookAccent = BookAccent.Dashboard) =
    com.selendang3.app.core.theme.estateColorScheme(dark, accent)
