package com.geuphalttaen.domain.sync

import java.io.InputStream
import java.nio.charset.Charset

interface ToiletDataPort {
    fun fetchFromStream(inputStream: InputStream, charset: Charset = Charset.forName("EUC-KR")): ToiletFetchResult
}

data class ToiletFetchResult(
    val items: List<ExternalToiletData>,
    val parseFailCount: Int = 0,
)

data class ExternalToiletData(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val male: Boolean = true,
    val female: Boolean = true,
    val disabled: Boolean = false,
    val familyRoom: Boolean = false,
)
