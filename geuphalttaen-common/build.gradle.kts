plugins {
    kotlin("plugin.spring")
}

// No bootJar — this is a library module
tasks.findByName("bootJar")?.enabled = false
tasks.findByName("jar")?.enabled = true

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.slf4j:slf4j-api")
    implementation("jakarta.validation:jakarta.validation-api")
}
