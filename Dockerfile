FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY geuphalttaen-common/build.gradle.kts geuphalttaen-common/build.gradle.kts
COPY geuphalttaen-core/build.gradle.kts geuphalttaen-core/build.gradle.kts
COPY geuphalttaen-domain/build.gradle.kts geuphalttaen-domain/build.gradle.kts
COPY geuphalttaen-infra/build.gradle.kts geuphalttaen-infra/build.gradle.kts
COPY geuphalttaen-api/build.gradle.kts geuphalttaen-api/build.gradle.kts
RUN ./gradlew dependencies --no-daemon -q || true

COPY geuphalttaen-common/src geuphalttaen-common/src
COPY geuphalttaen-core/src geuphalttaen-core/src
COPY geuphalttaen-domain/src geuphalttaen-domain/src
COPY geuphalttaen-infra/src geuphalttaen-infra/src
COPY geuphalttaen-api/src geuphalttaen-api/src
RUN ./gradlew :geuphalttaen-api:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
# cwebp: scrimage-webp WebP 변환에 필요한 네이티브 바이너리
RUN apt-get update && apt-get install -y --no-install-recommends webp && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/geuphalttaen-api/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-Djava.net.preferIPv6Addresses=true", "-jar", "app.jar"]
