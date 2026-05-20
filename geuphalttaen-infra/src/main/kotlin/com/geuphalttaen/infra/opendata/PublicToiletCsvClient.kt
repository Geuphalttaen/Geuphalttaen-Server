package com.geuphalttaen.infra.opendata

import com.geuphalttaen.domain.sync.ExternalToiletData
import com.geuphalttaen.domain.sync.ToiletDataPort
import com.geuphalttaen.domain.sync.ToiletFetchResult
import com.opencsv.CSVReaderBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

/**
 * 행정안전부 localdata.go.kr CSV 직접 다운로드 방식 화장실 데이터 클라이언트.
 * URL: https://file.localdata.go.kr/file/download/public_restroom_info/info
 */
@Component
open class PublicToiletCsvClient(
    private val properties: OpendataProperties,
) : ToiletDataPort {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 테스트에서 오버라이드 가능하도록 open으로 노출 */
    open fun openCsvStream(): Pair<InputStream, Charset> {
        val connection = URI(properties.csvUrl).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connect()

        val charset = connection.contentType
            ?.let { Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
            ?.let { runCatching { Charset.forName(it) }.getOrNull() }
            ?: Charset.forName("EUC-KR")

        return connection.inputStream to charset
    }

    override fun fetchAllToilets(): ToiletFetchResult {
        log.info("공공데이터 화장실 CSV 다운로드 시작: {}", properties.csvUrl)

        val (inputStream, charset) = openCsvStream()
        val reader = CSVReaderBuilder(InputStreamReader(inputStream, charset)).build()

        val headers = reader.readNext()?.map { it.trim() }
        if (headers == null) {
            log.warn("CSV 헤더를 읽을 수 없습니다.")
            return ToiletFetchResult(emptyList(), 0)
        }
        val idx = headers.withIndex().associate { (i, h) -> h to i }

        val items = mutableListOf<ExternalToiletData>()
        var parseFailCount = 0

        for (row in reader) {
            val data = parseRow(row, idx)
            if (data != null) items.add(data) else parseFailCount++
        }

        log.info("공공데이터 화장실 CSV 파싱 완료: 정상={}건, 파싱실패={}건", items.size, parseFailCount)
        return ToiletFetchResult(items = items, parseFailCount = parseFailCount)
    }

    private fun parseRow(row: Array<String>, idx: Map<String, Int>): ExternalToiletData? {
        fun col(name: String) = idx[name]?.let { row.getOrNull(it)?.trim() }

        val lat = col("위도")?.toDoubleOrNull() ?: return null
        val lng = col("경도")?.toDoubleOrNull() ?: return null

        val address = col("도로명주소")?.takeIf { it.isNotBlank() }
            ?: col("지번주소")?.takeIf { it.isNotBlank() }
            ?: return null

        val male = (col("남성용-대변기수")?.toIntOrNull() ?: 0) > 0
        val female = (col("여성용-대변기수")?.toIntOrNull() ?: 0) > 0
        val disabled = ((col("장애인용-남변기수")?.toIntOrNull() ?: 0)
                + (col("장애인용-여변기수")?.toIntOrNull() ?: 0)) > 0
        val familyRoom = ((col("영유아보육시설")?.toIntOrNull() ?: 0)
                + (col("수유실")?.toIntOrNull() ?: 0)) > 0

        return ExternalToiletData(
            name = col("화장실명")?.ifBlank { address } ?: address,
            address = address,
            lat = lat,
            lng = lng,
            male = male,
            female = female,
            disabled = disabled,
            familyRoom = familyRoom,
        )
    }
}
