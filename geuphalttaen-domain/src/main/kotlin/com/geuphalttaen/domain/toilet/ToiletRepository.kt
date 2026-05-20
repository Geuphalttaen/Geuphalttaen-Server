package com.geuphalttaen.domain.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ToiletRepository {
    fun findNearby(lat: Double, lng: Double, radiusMeters: Int): List<ToiletEntity>
    fun findById(id: Long): ToiletEntity?
    fun findByAddress(address: String): ToiletEntity?
    fun findAllByAddressIn(addresses: List<String>): List<ToiletEntity>
    fun findByReportedByOrderByCreatedAtDesc(reportedBy: Long): List<ToiletEntity>
    fun countByReportedBy(reportedBy: Long): Long
    fun countByReportedByAndStatus(reportedBy: Long, status: ToiletStatus): Long
    fun save(entity: ToiletEntity): ToiletEntity
    fun saveAll(entities: List<ToiletEntity>): List<ToiletEntity>
    fun delete(entity: ToiletEntity)

    /**
     * 관리자용 페이징 조회 (status 필터 옵션).
     */
    fun findByStatusPageable(status: ToiletStatus?, pageable: Pageable): Page<ToiletEntity>

    /**
     * 관리자용 키워드 페이징 조회 (이름 또는 주소 포함).
     */
    fun findByKeywordPageable(keyword: String?, pageable: Pageable): Page<ToiletEntity>

    /**
     * 특정 상태의 화장실 수를 반환한다.
     */
    fun countByStatus(status: ToiletStatus): Long

    /**
     * 공공데이터 출처(reportedBy=null) ACTIVE 화장실 전체 조회.
     * 동기화 후 CSV에서 사라진 항목을 인메모리 필터로 걸러 삭제하는 용도.
     */
    fun findAllActivePublic(): List<ToiletEntity>

    fun deleteAll(entities: List<ToiletEntity>)
}
