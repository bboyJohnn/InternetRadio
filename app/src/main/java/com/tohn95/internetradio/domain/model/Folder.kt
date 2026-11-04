package com.tohn95.internetradio.domain.model

/** Папка избранного со станциями в пользовательском порядке. */
data class Folder(
    val id: Long,
    val name: String,
    val stations: List<Station>,
    val coverPath: String? = null,
    val createdAt: Long = 0,
)
