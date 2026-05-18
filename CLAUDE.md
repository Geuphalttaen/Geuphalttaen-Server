# 급할땐 (Geuphalttaen) — Server

공중화장실 찾기 앱의 백엔드 서버입니다.

## 프로젝트 구조

Kotlin 2.1.0 + Spring Boot 3.4.5 + Gradle 9.3.1 멀티모듈 프로젝트.

```
geuphalttaen-common   — ApiResponse 봉투, ErrorCode, BusinessException, GlobalExceptionHandler
geuphalttaen-core     — JPA 엔티티(UserEntity, ToiletEntity), QueryDSL
geuphalttaen-domain   — 도메인 서비스(ToiletService, AuthService), DTO, JwtProvider
geuphalttaen-infra    — 외부 연동 (공공데이터 API 클라이언트, ToiletRepositoryImpl)
geuphalttaen-api      — REST 컨트롤러, Spring Security, Swagger, main (bootJar)
```

의존 방향: `api → {common, core, domain, infra}` / `infra → {common, core, domain}` / `domain → {common, core}` / `core → common`

## 주요 기술 스택

- Java 21
- MySQL 8 + Redis 7 (ddl-auto=update, 프로덕션은 수동 DDL)
- QueryDSL 5.1.0 (kapt 코드 생성)
- Spring Security + JWT (jjwt 0.12.x)
- springdoc-openapi 2.8.x (Swagger UI: `/swagger-ui.html`)

## Git Convention

- 브랜치: `feature/{기능}` / `fix/{버그}` / `refactor/{기능}` / `chore/{작업}`
- 커밋: `feat` / `fix` / `refactor` / `style` / `test` / `docs` / `chore`
- 흐름: feature → develop (PR) → main (릴리즈)

## 로컬 개발 환경

```bash
# DB + Redis 실행
docker compose up -d

# 서버 실행 (local 프로필)
./gradlew :geuphalttaen-api:bootRun --args='--spring.profiles.active=local'
```

## API 엔드포인트

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/v1/toilets` | 없음 | 근처 화장실 검색 (`lat`, `lng`, `radiusMeters`) |
| POST | `/api/v1/toilets/report` | Bearer JWT | 화장실 제보 |
| POST | `/api/v1/auth/login` | 없음 | OAuth 로그인 (Kakao/Apple) |

## TODO

- `AuthService.login`: OAuth provider 토큰 검증 및 UserEntity upsert 구현
- `PublicToiletApiClient.fetchToilets`: 공공데이터 API 실제 호출 구현
- Redis를 이용한 refresh token 저장/검증
- Kakao OAuth, Apple OAuth 클라이언트 구현
