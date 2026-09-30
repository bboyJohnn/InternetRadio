package com.tohn95.internetradio.domain.model

/** Папка избранного со станциями (новые сверху). */
data class Folder(val id: Long, val name: String, val stations: List<Station>)
