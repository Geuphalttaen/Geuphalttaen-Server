package com.geuphalttaen.api

import com.geuphalttaen.domain.auth.JwtProperties
import com.geuphalttaen.infra.ncp.NcpProperties
import com.geuphalttaen.infra.kakao.KakaoMapsProperties
import com.geuphalttaen.infra.oauth.KakaoProperties
import com.geuphalttaen.infra.opendata.OpendataProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = ["com.geuphalttaen"],
)
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["com.geuphalttaen"])
@EntityScan(basePackages = ["com.geuphalttaen"])
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(JwtProperties::class, OpendataProperties::class, KakaoProperties::class, NcpProperties::class, KakaoMapsProperties::class)
class GeuphalttaenApplication

fun main(args: Array<String>) {
    runApplication<GeuphalttaenApplication>(*args)
}
