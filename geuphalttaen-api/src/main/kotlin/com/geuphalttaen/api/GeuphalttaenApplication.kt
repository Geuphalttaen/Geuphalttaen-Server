package com.geuphalttaen.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication(
    scanBasePackages = ["com.geuphalttaen"],
)
@EnableJpaAuditing
class GeuphalttaenApplication

fun main(args: Array<String>) {
    runApplication<GeuphalttaenApplication>(*args)
}
