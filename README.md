# 급할땐 — Server

외출 중 가장 가까운 공중화장실을 빠르게 찾아주는 앱의 백엔드 서버

## 주요 기능

- 현재 위치 기반 근처 공중화장실 검색 (공공데이터 API 연동)
- 비공용 화장실 제보 접수 및 관리자 승인 처리
- 화장실 리뷰 / 청결도 평가 등록·수정
- Kakao / Apple OAuth2 → 서버 JWT 발급
- 회원 탈퇴 (리뷰·청결도·계정 삭제, 화장실 데이터 보존)
- 관리자 API (제보 승인/거절, 공공데이터 동기화)

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Kotlin 2.1.0 (JVM 21) |
| 프레임워크 | Spring Boot 3.4.5 |
| 빌드 | Gradle 9.3.1 (Kotlin DSL, 멀티모듈) |
| DB | MySQL 8.0 + Redis 7 |
| ORM | Spring Data JPA + QueryDSL 5.1.0 |
| 인증 | Spring Security + jjwt 0.12.x |
| 이미지 | Cloudflare R2 |
| API 문서 | springdoc-openapi 2.8.x |
| 테스트 | JUnit 5 + Mockito |

## 모듈 구조

| 모듈 | 역할 |
|------|------|
| `geuphalttaen-common` | ApiResponse 봉투, ErrorCode, 전역 예외 처리 |
| `geuphalttaen-core` | JPA 엔티티, QueryDSL Q클래스 |
| `geuphalttaen-domain` | 도메인 서비스, DTO, JwtProvider |
| `geuphalttaen-infra` | 외부 연동 구현체 (JPA Repository, OAuth, R2, 공공데이터 API) |
| `geuphalttaen-api` | REST Controller, Spring Security, Swagger, 진입점 |

## 시작하기

### 사전 준비

- JDK 21
- Docker (MySQL, Redis 실행용)

### 환경 변수

`geuphalttaen-api/src/main/resources/application-local.yml` 에 로컬 설정 작성 (`.gitignore` 적용됨)

### 실행

```bash
# DB + Redis 실행
docker compose up -d

# 서버 실행 (local 프로필)
./gradlew :geuphalttaen-api:bootRun --args='--spring.profiles.active=local'
```

Swagger UI: http://localhost:8080/swagger-ui.html

### 빌드 / 테스트

```bash
# 전체 빌드 + 테스트
./gradlew build

# 도메인 모듈 테스트만
./gradlew :geuphalttaen-domain:test
```

## 주요 API

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | — | OAuth 로그인 (Kakao/Apple) |
| POST | `/api/v1/auth/refresh` | — | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | Bearer | 로그아웃 |
| GET | `/api/v1/toilets` | — | 근처 화장실 검색 |
| GET | `/api/v1/toilets/{id}` | — | 화장실 상세 조회 |
| POST | `/api/v1/toilets/report` | Bearer | 화장실 제보 |
| GET | `/api/v1/users/me` | Bearer | 내 프로필 조회 |
| PATCH | `/api/v1/users/me` | Bearer | 닉네임 수정 |
| DELETE | `/api/v1/users/me` | Bearer | 회원 탈퇴 |
| GET | `/api/v1/users/me/reports` | Bearer | 내 제보 목록 |

## 앱 클라이언트

[geuphalttaen-app](../geuphalttaen-new-app) — Expo (React Native + TypeScript)
