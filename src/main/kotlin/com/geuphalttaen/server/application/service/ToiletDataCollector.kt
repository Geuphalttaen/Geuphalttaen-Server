package com.geuphalttaen.server.application.service

import com.geuphalttaen.server.adapter.out.opendata.PublicToiletApiClient
import com.geuphalttaen.server.adapter.out.opendata.dto.PublicToiletApiResponse
import com.geuphalttaen.server.application.port.out.ToiletRepository
import com.geuphalttaen.server.domain.Toilet
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class ToiletDataCollector(
    private val apiClient: PublicToiletApiClient,
    private val toiletRepository: ToiletRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        if (toiletRepository.count() == 0L) {
            log.info("화장실 데이터가 없습니다. 초기 수집을 시작합니다.")
            collect()
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    @Transactional
    fun collect() {
        val items = apiClient.fetchAll()
        val toilets = items.mapNotNull { it.toEntity() }
        toiletRepository.saveAll(toilets)
        log.info("화장실 데이터 저장 완료. ${toilets.size}건")
    }

    private fun PublicToiletApiResponse.Item.toEntity(): Toilet? {
        val lat = latitude?.toDoubleOrNull() ?: return null
        val lng = longitude?.toDoubleOrNull() ?: return null
        val name = toiletNm ?: return null

        return Toilet(
            name = name,
            roadAddress = rdnmadr ?: "",
            address = lnmadr ?: "",
            latitude = lat,
            longitude = lng,
            maleToiletCount = menToiletBowlNumber?.toIntOrNull() ?: 0,
            femaleToiletCount = womenToiletBowlNumber?.toIntOrNull() ?: 0,
            disabledMaleToiletCount = menHandicapToiletBowlNumber?.toIntOrNull() ?: 0,
            disabledFemaleToiletCount = womenHandicapToiletBowlNumber?.toIntOrNull() ?: 0,
            openTime = parseTime(openTime),
            closeTime = parseTime(closeTime),
            isDisabledAvailable = (menHandicapToiletBowlNumber?.toIntOrNull() ?: 0) > 0 ||
                (womenHandicapToiletBowlNumber?.toIntOrNull() ?: 0) > 0,
            managementAgency = institutionNm,
            phoneNumber = phoneNumber,
        )
    }

    private fun parseTime(time: String?): LocalTime? {
        if (time.isNullOrBlank()) return null
        return try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            null
        }
    }
}
