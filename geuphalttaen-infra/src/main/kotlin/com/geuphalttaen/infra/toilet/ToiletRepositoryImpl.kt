package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.QToiletEntity
import com.geuphalttaen.core.entity.ToiletEntity
import com.geuphalttaen.core.entity.ToiletStatus
import com.geuphalttaen.domain.toilet.ToiletRepository
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class ToiletRepositoryImpl(
    private val jpaRepository: ToiletJpaRepository,
    private val em: EntityManager,
) : ToiletRepository {

    private val queryFactory: JPAQueryFactory by lazy { JPAQueryFactory(em) }

    override fun findNearby(lat: Double, lng: Double, radiusMeters: Int): List<ToiletEntity> {
        val toilet = QToiletEntity.toiletEntity
        val distanceExpr = Expressions.numberTemplate(
            java.lang.Double::class.java,
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

    override fun findById(id: Long): ToiletEntity? = jpaRepository.findById(id).orElse(null)

    override fun findByNameAndAddress(name: String, address: String): ToiletEntity? =
        jpaRepository.findByNameAndAddress(name, address)

    override fun save(entity: ToiletEntity): ToiletEntity = jpaRepository.save(entity)
}
