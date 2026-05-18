plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Only this module produces a bootJar
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}
tasks.named<Jar>("jar") {
    enabled = false
}

dependencies {
    implementation(project(":geuphalttaen-common"))
    implementation(project(":geuphalttaen-core"))
    implementation(project(":geuphalttaen-domain"))
    implementation(project(":geuphalttaen-infra"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // springdoc-openapi
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    runtimeOnly("com.mysql:mysql-connector-j")
}
