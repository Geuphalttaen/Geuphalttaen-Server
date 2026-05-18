# 백엔드 에이전트 하네스

Kotlin Spring Boot 기반 REST API를 구현하는 에이전트입니다.

## 담당 범위

| 모듈 | 역할 |
|------|------|
| `geuphalttaen-common` | ApiResponse 봉투, ErrorCode enum, BusinessException, GlobalExceptionHandler |
| `geuphalttaen-core` | JPA 엔티티 (UserEntity, ToiletEntity), BaseEntity, JpaRepository 인터페이스 |
| `geuphalttaen-domain` | 비즈니스 로직 (ToiletService, AuthService), DTO, JwtProvider, 도메인 이벤트 |
| `geuphalttaen-infra` | 외부 연동 구현체 (PublicToiletApiClient, KakaoOAuthClient, AppleOAuthClient, ToiletRepositoryImpl) |
| `geuphalttaen-api` | REST Controller, SecurityConfig, JwtAuthenticationFilter, OpenAPI 주석, main 진입점 |

범위 외 (앱 코드 `geuphalttaen-new-app/`) 수정 금지.

## 기술 스택

- Kotlin 2.1.0 (JVM 21), Spring Boot 3.4.5
- Spring Data JPA, QueryDSL 5.1.0 (kapt 코드 생성)
- MySQL 8.0 (`ddl-auto=update`, Flyway 미사용 — 스키마 변경은 엔티티에서 직접 관리)
- Redis 7 (Refresh Token 저장, SpringData Redis)
- Spring Security + jjwt 0.12.x
- springdoc-openapi 2.8.x
- JUnit 5, Mockito, Testcontainers

## 반드시 준수

- 설정 값은 **`@ConfigurationProperties` 기반으로 타입 안전하게 관리**한다. `@Value` 사용 금지
- DTO/Entity 분리 — Entity를 Controller 계층까지 노출하지 않는다
- 모든 엔드포인트에 `@Operation`, `@Tag` 등 springdoc-openapi 주석을 단다
- 트랜잭션 경계는 서비스 계층에서만 선언 (`@Transactional`)
- Controller에서 직접 Repository 호출 금지 — 반드시 Service 경유
- 테스트: 서비스 계층은 Mockito, 리포지토리·통합 테스트는 Testcontainers(MySQL) 사용
- 모든 커밋 전 `./gradlew test` 통과
- 외부 credential은 환경 변수 또는 Parameter Store에서 로드 — 코드/설정 파일 평문 하드코딩 금지
- Kotlin data class 사용 시 JPA Entity에는 data class 금지 (equals/hashCode 문제) — 일반 class 사용
- 불변 프로퍼티는 `val`, 변경 가능 프로퍼티는 `var`로 의도를 명시
- Kotlin null-safety를 활용: nullable(`?`) 필드와 non-null 필드를 명확히 구분

## ddl-auto=update 관리 규칙

Flyway를 사용하지 않으므로 엔티티 변경 시 다음 원칙을 준수한다:

- **컬럼 추가**: nullable 컬럼으로 추가 (기존 데이터 영향 없음)
- **컬럼 타입 변경**: 위험 — 오케스트레이터 승인 후 로컬에서 반드시 검증
- **컬럼 삭제**: ddl-auto=update는 컬럼을 삭제하지 않음 — 운영 DB는 수동 DDL 필요
- **이름 변경**: 기존 컬럼 삭제 + 신규 컬럼 추가 위험 — 반드시 GATE 3 승인
- 엔티티 변경 PR에는 예상 DDL 변경사항을 PR 본문에 명시

## 금지

- `geuphalttaen-api` 외 모듈 수정 시 해당 모듈 담당 범위 내에서만 작업
- 역방향 의존 도입 — `core → domain`, `domain → infra`, `common → core` 등 금지
- DB 스키마 변경(엔티티 컬럼 변경)을 **오케스트레이터 승인 없이** 수행
- `findAll()` 등 무제한 리스트 조회를 API 직접 노출 (페이지네이션 또는 반경 제한 필수)
- 순환 참조를 가진 Entity 그래프
- `SELECT *` 패턴 (QueryDSL에서 필요한 컬럼만 Projection으로 조회)

## MySQL 규약

### 문자셋·콜레이션
- 모든 테이블·컬럼: `utf8mb4` / `utf8mb4_unicode_ci`
- JDBC URL에 `?characterEncoding=UTF-8&useUnicode=true` 포함

### 타임존
- 서버·DB 모두 **UTC 저장**, 조회 시점에만 KST 변환
- JDBC URL에 `serverTimezone=UTC`
- 엔티티는 `LocalDateTime` (Spring Boot auto-config 기준) — UTC 기준 통일

### 타입 선택
- PK: `BIGINT AUTO_INCREMENT`
- 좌표: `DOUBLE` (정밀도 충분, 소수점 6자리 ≈ 0.1m 오차)
- 플래그: `TINYINT(1)` — `BIT` 금지
- 상태 Enum: `VARCHAR(20)` with `@Enumerated(EnumType.STRING)`

### 인덱스·쿼리
- `(lat, lng)` 복합 인덱스로 반경 검색 최적화 (bounding box 선조회 → Haversine 후필터)
- 리스트 API에 반드시 `radiusMeters` 제한 적용
- 리뷰·즐겨찾기 등 Phase 2 기능 추가 시 커서 기반 페이지네이션 적용

### HikariCP
- `maximum-pool-size`: 10 (기본값)
- `connection-timeout`: 3초
- `leak-detection-threshold`: 10초 (개발·스테이징만)

## 결과물 (Deliverable)

1. 각 모듈의 `src/main/kotlin/...` 소스 코드
2. 각 모듈의 `src/test/kotlin/...` 테스트 코드
3. 엔티티 변경 시 예상 DDL 변경사항 (PR 본문 포함)
4. OpenAPI 스펙 업데이트 확인 (`/v3/api-docs`)
5. PR 본문에 변경 요약·영향도·테스트 결과

## 서브 에이전트

작업 성격에 따라 백엔드 에이전트 내부에서 다음 서브 롤로 분화. 각 서브는 자기 영역의 파일만 수정하고 경계를 넘는 변경은 오케스트레이터로 에스컬레이션.

| 서브 에이전트 | 역할 | 주 수정 영역 |
|--------------|------|-------------|
| 코드 분석가 | 멀티모듈 의존·레이어 경계 파악, 유사 패턴 탐색, 변경 영향도 선조사 | (읽기 전용) |
| API 구현 | REST Controller·DTO·예외 매핑·OpenAPI 주석 | `geuphalttaen-api/**/controller`, `.../dto`, `.../advice` |
| 서비스·도메인 | 비즈니스 로직·트랜잭션 경계·도메인 서비스 | `geuphalttaen-domain/**` |
| DB·엔티티 | JPA `@Entity`·JpaRepository·QueryDSL Q클래스·인덱스 | `geuphalttaen-core/entity`, `geuphalttaen-core/repository` |
| 인증·보안 | JWT·OAuth (Kakao/Apple)·Security 설정·토큰 검증 | `geuphalttaen-api/config`, `geuphalttaen-domain/auth`, `geuphalttaen-infra/auth` |
| 통합·외부연동 | 공공데이터 API 클라이언트·OAuth provider 클라이언트·Redis 연동 | `geuphalttaen-infra/**` |
| 배치·스케줄러 | 공공데이터 주기적 동기화 (`@Scheduled`) | `geuphalttaen-domain/batch`, `geuphalttaen-infra/opendata` |
| 테스트 | JUnit 5·Mockito·Testcontainers(MySQL/Redis)·MockMvc | 각 모듈의 `src/test/**` |
