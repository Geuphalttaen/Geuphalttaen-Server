package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.core.entity.SyncStatus
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
import java.io.ByteArrayInputStream

class ToiletSyncServiceTest {

    private lateinit var toiletDataPort: ToiletDataPort
    private lateinit var toiletRepository: ToiletRepository
    private lateinit var syncLogRepository: SyncLogRepository
    private lateinit var toiletSyncService: ToiletSyncService

    @BeforeEach
    fun setUp() {
        toiletDataPort = mock()
        toiletRepository = mock()
        syncLogRepository = mock()
        toiletSyncService = ToiletSyncService(toiletDataPort, toiletRepository, syncLogRepository)
        whenever(syncLogRepository.save(any())).thenAnswer { it.arguments[0] as SyncLogEntity }
        whenever(toiletRepository.saveAll(any<List<ToiletEntity>>())).thenAnswer { it.arguments[0] as List<ToiletEntity> }
        whenever(toiletRepository.findAllByAddressIn(any())).thenReturn(emptyList())
        whenever(toiletRepository.findAllActivePublicNotInAddresses(any())).thenReturn(emptyList())
    }

    private fun makeResult(vararg addresses: String): ToiletFetchResult = ToiletFetchResult(
        items = addresses.mapIndexed { i, addr ->
            ExternalToiletData("화장실$i", addr, 37.0 + i, 127.0 + i)
        },
        parseFailCount = 0,
    )

    private val emptyInputStream get() = ByteArrayInputStream("".toByteArray())

    @Test
    fun `syncFromUpload - 3개 신규 항목, insertedCount=3`() {
        val fetchResult = makeResult("주소1", "주소2", "주소3")
        whenever(toiletDataPort.fetchFromStream(any(), any())).thenReturn(fetchResult)
        whenever(toiletRepository.findAllByAddressIn(listOf("주소1", "주소2", "주소3"))).thenReturn(emptyList())

        val result = toiletSyncService.syncFromUpload(emptyInputStream)

        assertThat(result.totalFetched).isEqualTo(3)
        assertThat(result.insertedCount).isEqualTo(3)
        assertThat(result.updatedCount).isEqualTo(0)
        assertThat(result.deletedCount).isEqualTo(0)
        assertThat(result.failedCount).isEqualTo(0)
        assertThat(result.status).isEqualTo(SyncStatus.SUCCESS)
        verify(toiletRepository, times(1)).saveAll(any<List<ToiletEntity>>())
        verify(syncLogRepository, times(1)).save(any())
    }

    @Test
    fun `syncFromUpload - 빈 응답 시 모든 카운트 0, status=SUCCESS`() {
        whenever(toiletDataPort.fetchFromStream(any(), any())).thenReturn(ToiletFetchResult(emptyList()))
        whenever(toiletRepository.findAllByAddressIn(emptyList())).thenReturn(emptyList())

        val result = toiletSyncService.syncFromUpload(emptyInputStream)

        assertThat(result.totalFetched).isEqualTo(0)
        assertThat(result.insertedCount).isEqualTo(0)
        assertThat(result.updatedCount).isEqualTo(0)
        assertThat(result.deletedCount).isEqualTo(0)
        assertThat(result.failedCount).isEqualTo(0)
        assertThat(result.status).isEqualTo(SyncStatus.SUCCESS)
    }

    @Test
    fun `syncFromUpload - 기존 주소가 일치하면 updatedCount 증가`() {
        val existing = ToiletEntity(
            id = 10L,
            name = "구 이름",
            address = "서울시 강남구 주소1",
            lat = 37.0,
            lng = 127.0,
            status = ToiletStatus.ACTIVE,
        )
        val fetchResult = ToiletFetchResult(
            items = listOf(ExternalToiletData("새 이름", "서울시 강남구 주소1", 37.5, 127.5, male = true, female = false, disabled = true)),
        )
        whenever(toiletDataPort.fetchFromStream(any(), any())).thenReturn(fetchResult)
        whenever(toiletRepository.findAllByAddressIn(listOf("서울시 강남구 주소1"))).thenReturn(listOf(existing))

        val result = toiletSyncService.syncFromUpload(emptyInputStream)

        assertThat(result.insertedCount).isEqualTo(0)
        assertThat(result.updatedCount).isEqualTo(1)
        assertThat(result.status).isEqualTo(SyncStatus.SUCCESS)
        assertThat(existing.name).isEqualTo("새 이름")
        assertThat(existing.lat).isEqualTo(37.5)
        assertThat(existing.disabled).isTrue()
        assertThat(existing.isPublic).isTrue()
        assertThat(existing.status).isEqualTo(ToiletStatus.ACTIVE)
    }

    @Test
    fun `syncFromUpload - parseFailCount가 있으면 failedCount에 포함`() {
        val fetchResult = ToiletFetchResult(
            items = listOf(ExternalToiletData("화장실A", "주소1", 37.1, 127.1)),
            parseFailCount = 2,
        )
        whenever(toiletDataPort.fetchFromStream(any(), any())).thenReturn(fetchResult)
        whenever(toiletRepository.findAllByAddressIn(listOf("주소1"))).thenReturn(emptyList())

        val result = toiletSyncService.syncFromUpload(emptyInputStream)

        assertThat(result.totalFetched).isEqualTo(3)
        assertThat(result.insertedCount).isEqualTo(1)
        assertThat(result.failedCount).isEqualTo(2)
        assertThat(result.status).isEqualTo(SyncStatus.PARTIAL)
    }

    @Test
    fun `syncFromUpload - CSV에서 사라진 공공 항목 삭제, deletedCount 반영`() {
        val staleToilet = ToiletEntity(id = 99L, name = "삭제될화장실", address = "구 주소", lat = 37.0, lng = 127.0)
        whenever(toiletDataPort.fetchFromStream(any(), any())).thenReturn(makeResult("새 주소"))
        whenever(toiletRepository.findAllByAddressIn(any())).thenReturn(emptyList())
        whenever(toiletRepository.findAllActivePublicNotInAddresses(any())).thenReturn(listOf(staleToilet))

        val result = toiletSyncService.syncFromUpload(emptyInputStream)

        assertThat(result.deletedCount).isEqualTo(1)
        verify(toiletRepository, times(1)).deleteAll(listOf(staleToilet))
    }

    @Test
    fun `syncFromUpload - 데이터 파싱 실패 시 FAILED 상태로 저장`() {
        whenever(toiletDataPort.fetchFromStream(any(), any())).thenThrow(RuntimeException("파싱 실패"))

        val result = toiletSyncService.syncFromUpload(emptyInputStream)

        assertThat(result.status).isEqualTo(SyncStatus.FAILED)
        assertThat(result.errorMessage).isNotNull()
        verify(toiletRepository, never()).saveAll(any<List<ToiletEntity>>())
    }
}
