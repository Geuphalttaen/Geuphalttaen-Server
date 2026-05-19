package com.geuphalttaen.infra

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * 인프라 모듈 통합 테스트용 Spring Boot 애플리케이션 진입점.
 * 테스트 컨텍스트를 로드하기 위한 최소 구성이며, 실제 배포 시에는 사용되지 않는다.
 */
@SpringBootApplication(
    scanBasePackages = ["com.geuphalttaen.infra", "com.geuphalttaen.core"],
)
@EnableJpaAuditing
@EntityScan(basePackages = ["com.geuphalttaen.core.entity"])
@EnableJpaRepositories(basePackages = ["com.geuphalttaen.infra"])
@EnableConfigurationProperties
class InfraTestApplication
