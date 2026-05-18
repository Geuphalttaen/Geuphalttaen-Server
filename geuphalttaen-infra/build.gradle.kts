plugins {
    kotlin("plugin.spring")
    kotlin("kapt")
}

tasks.findByName("bootJar")?.enabled = false
tasks.findByName("jar")?.enabled = true

dependencies {
    implementation(project(":geuphalttaen-common"))
    implementation(project(":geuphalttaen-core"))
    implementation(project(":geuphalttaen-domain"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    // jjwt (Apple OAuth 토큰 검증)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Jackson Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly("com.mysql:mysql-connector-j")
}
