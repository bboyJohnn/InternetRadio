package com.tohn95.internetradio.domain.model

data class Station(
    val uuid: String, val name: String, val streamUrl: String, val faviconUrl: String?,
    val tags: List<String>, val country: String, val countryCode: String,
    val bitrate: Int, val codec: String, val votes: Int, val clickCount: Int, val isHls: Boolean,
    val language: String = "", val homepage: String? = null, val lastCheckOk: Boolean = true,
) {
    /** Станция, добавленная вручную по ссылке: её нет в каталоге radio-browser (клики/голоса не шлём). */
    val isCustom: Boolean get() = uuid.startsWith(CUSTOM_PREFIX)

    companion object { const val CUSTOM_PREFIX = "custom-" }
}
