package com.geuphalttaen.infra.opendata

import com.geuphalttaen.domain.sync.ExternalToiletData
import com.geuphalttaen.domain.sync.ToiletDataPort
import com.geuphalttaen.domain.sync.ToiletFetchResult
import com.opencsv.CSVReaderBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

@Component
class PublicToiletCsvClient : ToiletDataPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetchFromStream(inputStream: InputStream, charset: Charset): ToiletFetchResult {
        log.info("공공데이터 화장실 CSV 스트림 파싱 시작")

        val reader = CSVReaderBuilder(InputStreamReader(inputStream, charset)).build()

        val headers = reader.readNext()?.map { it.trim() }
        if (headers == null) {
            log.warn("CSV 헤더를 읽을 수 없습니다.")
            return ToiletFetchResult(emptyList(), 0)
        }
        log.info("CSV 헤더 목록: {}", headers)
        val idx = headers.withIndex().associate { (i, h) -> h to i }

        val items = mutableListOf<ExternalToiletData>()
        var noCoords = 0
        var noAddress = 0

        for (row in reader) {
            fun col(name: String) = idx[name]?.let { row.getOrNull(it)?.trim() }
            val lat = (col("WGS84위도") ?: col("위도"))?.toDoubleOrNull()
            val lng = (col("WGS84경도") ?: col("경도"))?.toDoubleOrNull()
            if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) { noCoords++; continue }
            val hasAddress = listOf("소재지도로명주소", "도로명주소", "소재지지번주소", "지번주소")
                .any { col(it)?.isNotBlank() == true }
            if (!hasAddress) { noAddress++; continue }
            val data = parseRow(row, idx)
            if (data != null) items.add(data)
        }

        val parseFailCount = noCoords + noAddress
        log.info("공공데이터 화장실 CSV 파싱 완료: 정상={}건, 좌표없음={}건, 주소없음={}건", items.size, noCoords, noAddress)
        return ToiletFetchResult(items = items, parseFailCount = parseFailCount)
    }

    private fun parseRow(row: Array<String>, idx: Map<String, Int>): ExternalToiletData? {
        fun col(name: String) = idx[name]?.let { row.getOrNull(it)?.trim() }

        val lat = (col("WGS84위도") ?: col("위도"))?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: return null
        val lng = (col("WGS84경도") ?: col("경도"))?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: return null

        val address = col("소재지도로명주소")?.takeIf { it.isNotBlank() }
            ?: col("도로명주소")?.takeIf { it.isNotBlank() }
            ?: col("소재지지번주소")?.takeIf { it.isNotBlank() }
            ?: col("지번주소")?.takeIf { it.isNotBlank() }
            ?: return null

        val male = (col("남성용-대변기수")?.toIntOrNull() ?: 0) > 0
        val female = (col("여성용-대변기수")?.toIntOrNull() ?: 0) > 0
        val disabled = ((col("남성용-장애인용대변기수")?.toIntOrNull()
            ?: col("장애인용-남변기수")?.toIntOrNull() ?: 0)
                + (col("여성용-장애인용대변기수")?.toIntOrNull()
            ?: col("장애인용-여변기수")?.toIntOrNull() ?: 0)) > 0
        val familyRoom = col("기저귀교환대유무")?.trim().equals("Y", ignoreCase = true)
            || (col("기저귀교환대남자화장실")?.toIntOrNull() ?: 0) > 0
            || (col("영유아보육시설")?.toIntOrNull() ?: 0) > 0
            || col("수유실설치여부")?.trim().equals("Y", ignoreCase = true)
            || (col("수유실")?.toIntOrNull() ?: 0) > 0

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
