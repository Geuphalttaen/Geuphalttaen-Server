plugins {
    kotlin("plugin.spring")
}

tasks.findByName("bootJar")?.enabled = false
tasks.findByName("jar")?.enabled = true

dependencies {
    implementation(project(":geuphalttaen-common"))
    implementation(project(":geuphalttaen-core"))

    implementation("org.springframework.boot:spring-boot-starter-security")

    // jjwt
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}
