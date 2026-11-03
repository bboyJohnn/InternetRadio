package com.tohn95.internetradio.playback

import com.tohn95.internetradio.domain.model.Station
import kotlin.random.Random

/**
 * Очередь ⏮ ⏭: откуда включили, в каком порядке листать, перемешана ли. Без Android — проверяется тестами;
 * [PlayerController] только передаёт ей события. Источник [PlaySource.None] листает по Избранному (fallback).
 */
class QueueState(private val random: Random = Random.Default) {
    var source: PlaySource = PlaySource.None
        private set
    /** Порядок, по которому листаем сейчас (перемешанный, если включено). */
    var stations: List<Station> = emptyList()
        private set
    var shuffled: Boolean = false
        private set

    private var original: List<Station> = emptyList()
    /** Где играющая стояла, когда последний раз была в списке, — чтобы листать «от её места», если её убрали. */
    private var anchor = -1

    fun start(queue: PlayQueue?, station: Station) {
        source = queue?.source ?: PlaySource.None
        original = queue?.stations.orEmpty()
        stations = original
        shuffled = false
        anchor = stations.indexOfFirst { it.uuid == station.uuid }
    }

    fun next(currentUuid: String?, direction: Int, fallback: List<Station>): Station? {
        val list = listFor(fallback)
        list.indexOfFirst { it.uuid == currentUuid }.takeIf { it >= 0 }?.let { anchor = it }
        return neighbor(list, currentUuid, anchor, direction)
    }

    fun hasNeighbors(currentUuid: String?, fallback: List<Station>): Boolean =
        hasNeighbors(listFor(fallback), currentUuid)

    /** Список источника поменяли (переставили, убрали, добавили) — только если играем именно из него. */
    fun update(source: PlaySource, fresh: List<Station>, currentUuid: String?) {
        if (source != this.source || source == PlaySource.None) return
        original = fresh
        stations = if (shuffled) reconcile(stations, fresh) else fresh
        stations.indexOfFirst { it.uuid == currentUuid }.takeIf { it >= 0 }?.let { anchor = it }
    }

    /** Перемешать: играющая остаётся первой, остальные — случайно; выключить — вернуть порядок источника. */
    fun setShuffle(on: Boolean, currentUuid: String?) {
        if (on == shuffled || source == PlaySource.None) return
        shuffled = on
        stations = if (on) {
            val (current, rest) = original.partition { it.uuid == currentUuid }
            current + rest.shuffled(random)
        } else original
        anchor = stations.indexOfFirst { it.uuid == currentUuid }
    }

    private fun listFor(fallback: List<Station>) = if (source == PlaySource.None) fallback else stations
}
