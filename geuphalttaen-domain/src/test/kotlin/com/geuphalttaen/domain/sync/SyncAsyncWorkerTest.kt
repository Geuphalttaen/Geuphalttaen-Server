package com.geuphalttaen.domain.sync

import com.geuphalttaen.core.entity.SyncLogEntity
import com.geuphalttaen.core.entity.SyncStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.charset.Charset

class SyncAsyncWorkerTest {

    private lateinit var toiletSyncService: ToiletSyncService
    private lateinit var syncLogRepository: SyncLogRepository
    private lateinit var syncAsyncWorker: SyncAsyncWorker

    private val charset = Charset.forName("EUC-KR")

    @BeforeEach
    fun setUp() {
        toiletSyncService = mock()
        syncLogRepository = mock()
        syncAsyncWorker = SyncAsyncWorker(toiletSyncService, syncLogRepository)
    }

    private fun inProgressLog(id: Long = 1L) = SyncLogEntity(id = id, status = SyncStatus.IN_PROGRESS)

    @Test
    fun `run - 동기화 성공 시 syncLog 를 SUCCESS 로 업데이트`() {
        val syncLog = inProgressLog()
        val result = SyncLogEntity(
            totalFetched = 100,
            upsertedCount = 90,
            insertedCount = 80,
            updatedCount = 10,
            deletedCount = 5,
            failedCount = 0,
            status = SyncStatus.SUCCESS,
        )
        whenever(syncLogRepository.findById(1L)).thenReturn(syncLog)
        whenever(toiletSyncService.runSync(any(), any())).thenReturn(result)
        whenever(syncLogRepository.save(any())).thenAnswer { it.arguments[0] as SyncLogEntity }

        syncAsyncWorker.run(1L, ByteArray(0), charset)

        val captor = argumentCaptor<SyncLogEntity>()
        verify(syncLogRepository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.status).isEqualTo(SyncStatus.SUCCESS)
        assertThat(saved.totalFetched).isEqualTo(100)
        assertThat(saved.insertedCount).isEqualTo(80)
        assertThat(saved.updatedCount).isEqualTo(10)
        assertThat(saved.deletedCount).isEqualTo(5)
        assertThat(saved.failedCount).isEqualTo(0)
    }

    @Test
    fun `run - runSync 가 FAILED 반환 시 syncLog 를 FAILED 로 업데이트`() {
        val syncLog = inProgressLog()
        val result = SyncLogEntity(
            totalFetched = 0,
            status = SyncStatus.FAILED,
            errorMessage = "파싱 실패",
        )
        whenever(syncLogRepository.findById(1L)).thenReturn(syncLog)
        whenever(toiletSyncService.runSync(any(), any())).thenReturn(result)
        whenever(syncLogRepository.save(any())).thenAnswer { it.arguments[0] as SyncLogEntity }

        syncAsyncWorker.run(1L, ByteArray(0), charset)

        val captor = argumentCaptor<SyncLogEntity>()
        verify(syncLogRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(SyncStatus.FAILED)
        assertThat(captor.firstValue.errorMessage).isEqualTo("파싱 실패")
    }

    @Test
    fun `run - runSync 예외 발생 시 syncLog 를 FAILED 로 업데이트`() {
        val syncLog = inProgressLog()
        whenever(syncLogRepository.findById(1L)).thenReturn(syncLog)
        whenever(toiletSyncService.runSync(any(), any())).thenThrow(RuntimeException("예기치 못한 오류"))
        whenever(syncLogRepository.save(any())).thenAnswer { it.arguments[0] as SyncLogEntity }

        syncAsyncWorker.run(1L, ByteArray(0), charset)

        val captor = argumentCaptor<SyncLogEntity>()
        verify(syncLogRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(SyncStatus.FAILED)
        assertThat(captor.firstValue.errorMessage).contains("예기치 못한 오류")
    }

    @Test
    fun `run - syncLog 를 찾지 못하면 아무 것도 저장하지 않음`() {
        whenever(syncLogRepository.findById(99L)).thenReturn(null)

        syncAsyncWorker.run(99L, ByteArray(0), charset)

        verify(syncLogRepository, org.mockito.kotlin.never()).save(any())
    }
}
