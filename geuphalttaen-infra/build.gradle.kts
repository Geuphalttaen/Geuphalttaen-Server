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
    implementation("org.springframework.boot:spring-boot-starter-validation")

    kapt("org.springframework.boot:spring-boot-configuration-processor")


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

    // CSV 파싱
    implementation("com.opencsv:opencsv:5.9")

    // Cloudflare R2 (AWS SDK S3 호환)
    implementation(platform("software.amazon.awssdk:bom:2.25.60"))
    implementation("software.amazon.awssdk:s3")

    // WebP 변환 (cwebp 네이티브 바이너리 필요)
    implementation("com.sksamuel.scrimage:scrimage-core:4.3.0")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.3.0")

    runtimeOnly("com.mysql:mysql-connector-j")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}
