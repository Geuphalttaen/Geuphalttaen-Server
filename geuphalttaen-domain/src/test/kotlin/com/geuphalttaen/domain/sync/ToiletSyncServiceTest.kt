package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class ToiletSyncServiceTest {

    private lateinit var toiletRepository: ToiletRepository
    private lateinit var syncLogRepository: SyncLogRepository
    private lateinit var service: ToiletSyncService

    @BeforeEach
    fun setUp() {
        toiletRepository = mock()
        syncLogRepository = mock()
        service = ToiletSyncService(toiletRepository, syncLogRepository)
        whenever(toiletRepository.save(any())).thenAnswer { it.arguments[0] as ToiletEntity }
        whenever(syncLogRepository.save(any())).thenAnswer { it.arguments[0] as SyncLogEntity }
    }

    @Test
    fun `신규 화장실은 ACTIVE 상태로 저장된다`() {
        val dto = SyncToiletDto("화장실A", "서울시 강남구", 37.1, 127.1, male = true, female = true, disabled = false)
        whenever(toiletRepository.findByNameAndAddress("화장실A", "서울시 강남구")).thenReturn(null)
        val result = service.syncPublicToilets(listOf(dto))
        assertThat(result.created).isEqualTo(1)
        assertThat(result.updated).isEqualTo(0)
        assertThat(result.skipped).isEqualTo(0)
        verify(toiletRepository, times(1)).save(any())
    }

    @Test
    fun `기존 화장실은 업데이트된다`() {
        val existing = ToiletEntity(id = 1L, name = "화장실A", address = "서울시 강남구", lat = 37.0, lng = 127.0, status = ToiletStatus.ACTIVE)
        val dto = SyncToiletDto("화장실A", "서울시 강남구", 37.1, 127.1, male = true, female = true, disabled = true)
        whenever(toiletRepository.findByNameAndAddress("화장실A", "서울시 강남구")).thenReturn(existing)
        val result = service.syncPublicToilets(listOf(dto))
        assertThat(result.updated).isEqualTo(1)
        assertThat(result.created).isEqualTo(0)
        assertThat(existing.lat).isEqualTo(37.1)
        assertThat(existing.disabled).isTrue()
        verify(toiletRepository, times(1)).save(existing)
    }

    @Test
    fun `lat이 0이면 건너뜀`() {
        val dto = SyncToiletDto("화장실A", "서울시 강남구", 0.0, 127.1, male = true, female = true, disabled = false)
        val result = service.syncPublicToilets(listOf(dto))
        assertThat(result.skipped).isEqualTo(1)
        assertThat(result.created).isEqualTo(0)
        verify(toiletRepository, never()).save(any())
    }

    @Test
    fun `lng이 0이면 건너뜀`() {
        val dto = SyncToiletDto("화장실B", "서울시 강남구", 37.1, 0.0, male = true, female = true, disabled = false)
        val result = service.syncPublicToilets(listOf(dto))
        assertThat(result.skipped).isEqualTo(1)
        assertThat(result.created).isEqualTo(0)
        verify(toiletRepository, never()).save(any())
    }

    @Test
    fun `혼합 목록 처리 - 신규 업데이트 건너뜀`() {
        val dtos = listOf(
            SyncToiletDto("신규화장실", "주소1", 37.1, 127.1, male = true, female = true, disabled = false),
            SyncToiletDto("기존화장실", "주소2", 37.2, 127.2, male = true, female = true, disabled = false),
            SyncToiletDto("좌표없음", "주소3", 0.0, 0.0, male = true, female = true, disabled = false),
        )
        val existing = ToiletEntity(id = 2L, name = "기존화장실", address = "주소2", lat = 37.0, lng = 127.0, status = ToiletStatus.ACTIVE)
        whenever(toiletRepository.findByNameAndAddress("신규화장실", "주소1")).thenReturn(null)
        whenever(toiletRepository.findByNameAndAddress("기존화장실", "주소2")).thenReturn(existing)
        val result = service.syncPublicToilets(dtos)
        assertThat(result.created).isEqualTo(1)
        assertThat(result.updated).isEqualTo(1)
        assertThat(result.skipped).isEqualTo(1)
        verify(toiletRepository, times(2)).save(any())
    }

    @Test
    fun `getLastSyncStatus - 동기화 기록이 있으면 반환`() {
        val logEntry = SyncLogEntity(syncedAt = LocalDateTime.of(2026, 5, 18, 3, 0), createdCount = 10, updatedCount = 5, skippedCount = 2)
        whenever(syncLogRepository.findTopByOrderBySyncedAtDesc()).thenReturn(logEntry)
        val status = service.getLastSyncStatus()
        assertThat(status).isNotNull
        assertThat(status!!.createdCount).isEqualTo(10)
        assertThat(status.updatedCount).isEqualTo(5)
        assertThat(status.skippedCount).isEqualTo(2)
    }

    @Test
    fun `getLastSyncStatus - 동기화 기록이 없으면 null 반환`() {
        whenever(syncLogRepository.findTopByOrderBySyncedAtDesc()).thenReturn(null)
        val status = service.getLastSyncStatus()
        assertThat(status).isNull()
    }
}
