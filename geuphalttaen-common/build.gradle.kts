plugins {
    kotlin("plugin.spring")
}

// No bootJar — this is a library module
tasks.findByName("bootJar")?.enabled = false
tasks.findByName("jar")?.enabled = true

dependencies {
    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("jakarta.validation:jakarta.validation-api")
}
