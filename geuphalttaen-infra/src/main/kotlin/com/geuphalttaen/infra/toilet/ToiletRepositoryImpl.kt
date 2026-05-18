package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.QToiletEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class ToiletRepositoryImpl(
    private val jpaRepository: ToiletJpaRepository,
    private val em: EntityManager,
) : ToiletRepository {

    private val queryFactory: JPAQueryFactory by lazy { JPAQueryFactory(em) }

    /**
     * MySQL ST_Distance_Sphere 함수를 사용한 근방 화장실 조회.
     * QueryDSL 에서 네이티브 함수 호출은 Expressions.numberTemplate 활용.
     */
    override fun findNearby(lat: Double, lng: Double, radiusMeters: Int): List<ToiletEntity> {
        val toilet = QToiletEntity.toiletEntity

        // ST_Distance_Sphere returns distance in meters
        val distanceExpr = com.querydsl.core.types.dsl.Expressions.numberTemplate(
            Double::class.java,
            "ST_Distance_Sphere(POINT({0}, {1}), POINT({2}, {3}))",
            lng, lat, toilet.lng, toilet.lat,
        )

        return queryFactory
            .selectFrom(toilet)
            .where(
                toilet.status.eq(ToiletStatus.ACTIVE),
                distanceExpr.loe(radiusMeters.toDouble()),
            )
            .orderBy(distanceExpr.asc())
            .fetch()
    }

    override fun save(entity: ToiletEntity): ToiletEntity = jpaRepository.save(entity)
}
