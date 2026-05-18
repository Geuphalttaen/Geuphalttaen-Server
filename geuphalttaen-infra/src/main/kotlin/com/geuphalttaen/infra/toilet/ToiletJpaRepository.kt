package com.geuphalttaen.infra.toilet

import com.geuphalttaen.core.entity.ToiletEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ToiletJpaRepository : JpaRepository<ToiletEntity, Long>
