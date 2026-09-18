package com.example.notebook

data class Note(
    val id: String = "",
    val authorUid: String = "",
    val anonymous: Boolean = false,
    val content: String = "",
    val template: String = "classic",
    val colorHex: String = "#FDF6ED",
    val dateDisplay: String = "",
    val createdAt: Long = 0L
)