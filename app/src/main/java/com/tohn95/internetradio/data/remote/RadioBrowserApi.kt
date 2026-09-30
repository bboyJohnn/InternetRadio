package com.tohn95.internetradio.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun search(
        @Query("name") name: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("tag") tag: String? = null,
        @Query("language") language: String? = null,
        @Query("codec") codec: String? = null,
        @Query("bitrateMin") bitrateMin: Int? = null,
        @Query("bitrateMax") bitrateMax: Int? = null,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<StationDto>

    @GET("json/countries")
    suspend fun countries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<CountryDto>

    @GET("json/tags")
    suspend fun tags(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("limit") limit: Int = 1000,
    ): List<NameCountDto>

    @GET("json/languages")
    suspend fun languages(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("limit") limit: Int = 100,
    ): List<NameCountDto>

    @GET("json/codecs")
    suspend fun codecs(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true,
    ): List<NameCountDto>

    @GET("json/url/{uuid}")
    suspend fun trackClick(@Path("uuid") uuid: String): Unit

    @GET("json/vote/{uuid}")
    suspend fun vote(@Path("uuid") uuid: String): VoteDto
}
