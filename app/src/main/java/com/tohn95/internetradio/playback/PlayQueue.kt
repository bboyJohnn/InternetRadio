package com.tohn95.internetradio.playback

import com.tohn95.internetradio.domain.model.Station

/** Откуда включили станцию: по этому списку листают ⏮ ⏭ и свайпы, его же подписывает экран станции. */
sealed interface PlaySource {
    data object Favorites : PlaySource
    data class Folder(val id: Long, val name: String) : PlaySource
    data class Search(val query: String) : PlaySource
    data class Genre(val title: String) : PlaySource
    /** Раздел Главной или список («Популярное», чарт, страна…). */
    data class Section(val title: String) : PlaySource
    data object History : PlaySource
    /** Включили одну станцию (по ссылке, «Мне повезёт»): листаем по Избранному. */
    data object None : PlaySource
}

data class PlayQueue(val source: PlaySource, val stations: List<Station>)

/**
 * Соседняя станция по кругу. Играющей в списке нет (её убрали) — считаем от места, где она стояла
 * ([anchor]): там теперь следующая, перед ним — предыдущая. Нет и места — к первой/последней.
 * Сама играющая «соседом» не бывает (список из неё одной) — null.
 */
fun neighbor(stations: List<Station>, currentUuid: String?, anchor: Int, direction: Int): Station? {
    if (stations.isEmpty()) return null
    val idx = stations.indexOfFirst { it.uuid == currentUuid }
    val next = when {
        idx >= 0 -> stations[(idx + direction).mod(stations.size)]
        anchor < 0 -> if (direction > 0) stations.first() else stations.last()
        direction > 0 -> stations[anchor.mod(stations.size)]
        else -> stations[(anchor - 1).mod(stations.size)]
    }
    return next.takeIf { it.uuid != currentUuid }
}

/** Есть ли куда переключиться (для приглушения ⏮ ⏭ и кнопок уведомления). */
fun hasNeighbors(stations: List<Station>, currentUuid: String?): Boolean =
    stations.size >= 2 || (stations.size == 1 && stations[0].uuid != currentUuid)

/** Перемешанная очередь после правки списка: прежний порядок без исчезнувших, новые — в конец. */
fun reconcile(oldOrder: List<Station>, fresh: List<Station>): List<Station> {
    val byUuid = fresh.associateBy { it.uuid }
    val kept = oldOrder.mapNotNull { byUuid[it.uuid] }
    val keptIds = kept.mapTo(HashSet()) { it.uuid }
    return kept + fresh.filter { it.uuid !in keptIds }
}
