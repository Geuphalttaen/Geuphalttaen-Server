# 급할땐 (Geuphalttaen) — Server

이 파일은 Claude Code가 이 저장소의 코드를 다룰 때 참고하는 전역 안내서입니다.

## 프로젝트 개요

**급할땐**은 외출 중 가장 가까운 공중화장실을 빠르게 찾아주는 앱의 백엔드 서버입니다.

- 비로그인 사용자: 현재 위치 기반 근처 공중화장실 지도 조회 (공공데이터 활용)
- 로그인 사용자: 비공용 화장실 포함 위치 제보 (관리자 승인 후 노출)
- 인증 방식: Kakao / Apple OAuth2 → 서버 JWT 발급
- 기획 문서: `docs/기획서.md`
- 프로젝트 단계: **Phase 1 (MVP)** — 2026-05 ~

## 모듈 구조

| 모듈 | 역할 |
|------|------|
| `geuphalttaen-common` | ApiResponse 봉투, ErrorCode, BusinessException, GlobalExceptionHandler |
| `geuphalttaen-core` | JPA 엔티티 (UserEntity, ToiletEntity), QueryDSL, BaseEntity |
| `geuphalttaen-domain` | 도메인 서비스 (ToiletService, AuthService), DTO, JwtProvider |
| `geuphalttaen-infra` | 외부 연동 구현체 (PublicToiletApiClient, ToiletRepositoryImpl, OAuth 클라이언트) |
| `geuphalttaen-api` | REST Controller, Spring Security, Swagger, main 진입점 (`bootJar`) |

**의존 방향**: `api → {common, core, domain, infra}` / `infra → {common, core, domain}` / `domain → {common, core}` / `core → common`

규칙: 하위 모듈이 상위 모듈을 참조하는 역방향 의존 금지.

## 기술 스택

- **언어**: Kotlin 2.1.0 (JVM 21)
- **프레임워크**: Spring Boot 3.4.5
- **빌드**: Gradle 9.3.1 (Kotlin DSL, 멀티모듈)
- **DB**: MySQL 8.0 + Redis 7 (`ddl-auto=update`, 프로덕션은 수동 DDL)
- **ORM**: Spring Data JPA + QueryDSL 5.1.0 (kapt 코드 생성)
- **인증**: Spring Security + jjwt 0.12.x
- **API 문서**: springdoc-openapi 2.8.x (Swagger UI: `/swagger-ui.html`)
- **테스트**: JUnit 5, Mockito, Testcontainers

## Git Convention

- **브랜치**: `feature/{기능}` / `fix/{버그}` / `refactor/{기능}` / `chore/{작업}`
- **커밋 타입**: `feat` / `fix` / `refactor` / `style` / `test` / `docs` / `chore`
- **흐름**: feature → develop (PR) → main (릴리즈)
- develop에 직접 push 금지 — 반드시 PR로 머지

## 로컬 개발 환경

```bash
# DB + Redis 실행
docker compose up -d

# 서버 실행 (local 프로필)
./gradlew :geuphalttaen-api:bootRun --args='--spring.profiles.active=local'

# 전체 빌드 + 테스트
./gradlew build

# 특정 모듈 테스트
./gradlew :geuphalttaen-domain:test
```

Swagger UI: http://localhost:8080/swagger-ui.html

## API 엔드포인트

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | 없음 | OAuth 로그인 (Kakao/Apple) |
| POST | `/api/v1/auth/refresh` | 없음 | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | Bearer JWT | 로그아웃 |
| GET | `/api/v1/toilets` | 없음 | 근처 화장실 검색 (`lat`, `lng`, `radiusMeters`) |
| GET | `/api/v1/toilets/{id}` | 없음 | 화장실 상세 조회 |
| POST | `/api/v1/toilets/report` | Bearer JWT | 화장실 위치 제보 |

## 에이전트 위임 가이드

작업을 서브 에이전트에 위임할 때 다음 원칙을 따릅니다.

- **API/DB/인증 관련** → `agents/backend.md`
  - REST 엔드포인트 추가/수정, JPA 엔티티, 스케줄러, 인증
  - 설정은 `@ConfigurationProperties` 기반으로 타입 안전하게 관리 (`@Value` 금지)
- **QA/테스트** → `agents/qa.md`
  - 시나리오 체크리스트, 통합 테스트, 보안 점검
  - Critical/High 버그 0건 확인 후 오케스트레이터에게 결과 반환
- **코드 리뷰** → `agents/reviewer.md`
  - PR 생성 직후 자동 실행, 블로커 + 개선권고 처리

## AI 에이전트 오케스트레이션 파이프라인

```
/기획시작 → GATE 1 승인
  → /설계시작 → GATE 2 승인
    → /backend시작
      ├── PR 생성 즉시: reviewer 에이전트 + QA 에이전트 병렬 실행
      │     → gh pr comment <PR번호> --body "<리뷰 결과>"
      │     → 블로커([B]) AND 개선권고([I]) 모두 즉시 반영 후 재커밋 push
      │     → reviewer + QA 즉시 재실행 (병렬)
      │     → 블로커 0건 + QA GATE 4 통과 → 유저에게 머지 요청
      └── GATE 3 (DB 스키마 변경 검증)
        → /qa시작
```

## GATE 승인 기준

| GATE | 조건 | 승인 방법 |
|------|------|------|
| GATE 1 (기획→설계) | 기획문서 완성, 미결사항 0건 | "기획 승인" 또는 "설계 시작해" |
| GATE 2 (설계→구현) | 설계 산출물 완성, API 스펙 합의 | "설계 승인" 또는 "구현 시작해" |
| GATE 3 (DB 스키마) | ddl-auto=update 리스크 검토, 엔티티 변경 범위 명시 | "DB 승인" 또는 자동 통과 |
| GATE 4 (QA→완료) | Critical/High 버그 0건 | QA 에이전트 자동 판단 |
| **PR 리뷰 게이트** | 모든 PR에 reviewer 에이전트 리뷰 코멘트 1회 이상, 블로커 0건 + **개선권고 전항목 반영** | 유저가 머지 버튼 |

## 에이전트 구성

| 에이전트 | 파일 | 역할 |
|----------|------|------|
| 백엔드 | `agents/backend.md` | REST API, DB, 스케줄러, 인증 구현 |
| QA | `agents/qa.md` | 시나리오, 통합, 보안 테스트 |
| 코드 리뷰 | `agents/reviewer.md` | PR 오픈 직후 독립 리뷰, 블로커 식별 |

## 코드 수정 및 테스트 규칙

### 수정-테스트 루프

```
수정 명세서 승인
  → 코드 수정
    → 단위 테스트 작성/실행
      → PASS (커버리지 80%+) → 커밋
      → FAIL → 원인 분석 → 코드 수정 → 단위 테스트 (반복, 최대 10회)
        → 10회 실패 → 설계 재검토 에스컬레이션
```

### 필수 규칙

- 코드 수정 시 반드시 해당 기능의 단위 테스트를 작성하거나 기존 테스트를 실행
- 테스트 커버리지 80% 미만이면 커밋 금지
- 수정 명세서에 명시되지 않은 파일/함수는 수정하지 않음
- 기존 코드의 리팩토링, 변수명 변경, import 정리 등 무관한 변경 금지

## 공통 코드 언어

코드 주석·문서·커밋 메시지의 기본 언어는 **한국어**

## 보조 커맨드

| 커맨드 | 용도 |
|--------|------|
| `/기획시작` | 요구사항 토론 → 기획문서 작성 |
| `/설계시작` | 아키텍처·API·DB 설계 |
| `/backend시작` | Spring Boot API 구현 |
| `/qa시작` | 시나리오·통합 테스트 |
| `/리뷰시작 <PR번호>` | PR 코드 리뷰 에이전트 호출 |
