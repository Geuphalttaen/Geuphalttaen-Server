package com.geuphalttaen.api

import com.geuphalttaen.domain.auth.JwtProperties
import com.geuphalttaen.infra.oauth.KakaoProperties
import com.geuphalttaen.infra.opendata.OpendataProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = ["com.geuphalttaen"],
)
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(JwtProperties::class, OpendataProperties::class, KakaoProperties::class)
class GeuphalttaenApplication

fun main(args: Array<String>) {
    runApplication<GeuphalttaenApplication>(*args)
}
