package com.example.notebook

data class DailyNote(
    val id: String = "",
    val authorUid: String = "",
    val content: String = "",
    val template: String = "classic",
    val colorHex: String = "#FDF6ED",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L
)