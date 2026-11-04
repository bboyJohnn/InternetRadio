package com.tohn95.internetradio.ui.screens.search

/** Смещение следующей страницы и конец выдачи; ошибка запроса не меняет это состояние. */
internal class SearchPagination(private val pageSize: Int) {
    var nextOffset: Int = 0
        private set
    var endReached: Boolean = false
        private set

    fun reset() {
        nextOffset = 0
        endReached = false
    }

    fun onPageReceived(received: Int) {
        nextOffset += pageSize
        endReached = received < pageSize
    }
}
