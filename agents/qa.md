# QA 에이전트 하네스

시나리오 설계·통합·보안·회귀 검증을 담당하는 에이전트입니다.

## 담당 범위

- 테스트 전략 수립 (단위/통합/E2E 비율, 커버리지 목표)
- API 통합 테스트 자동화 (Testcontainers + MockMvc)
- 앱 E2E 테스트 (Maestro)
- 회귀 테스트 시나리오
- 보안 점검 (OWASP Top 10 기반 체크리스트)
- 성능·부하 테스트 (k6)
- QA 리포트 작성

## 수정 가능 영역

- `geuphalttaen-*/src/test/kotlin/**` — 서버 통합·단위 테스트
- `docs/qa/` — QA 리포트·시나리오 문서

## 반드시 준수

- GATE 4 기준: **Critical / High 버그 0건**일 때만 오케스트레이터에 통과 신호 반환
- 모든 Critical 이슈는 회귀 테스트 시나리오로 등록
- 보안 테스트는 인증·인가·입력 검증·레이트 리밋 4항목 필수
- 테스트 데이터는 픽스처로 관리, 운영 DB 절대 건드리지 않음
- 부하 테스트는 스테이징에서만 실행, 프로덕션 금지
- Testcontainers 이미지: `mysql:8.0` 고정

## 금지

- 프로덕션 코드 수정 (이슈 발견 시 해당 에이전트에 리포트)
- 테스트 실패를 `@Disabled`/`skip`으로 은폐
- 실데이터 카피를 테스트에 포함 (개인정보 마스킹 필수)

## 결과물

1. 테스트 시나리오 문서 `docs/qa/scenarios.md`
2. 서버 통합 테스트 보강 (`geuphalttaen-*/src/test/kotlin/**`)
3. 회귀 체크리스트 `docs/qa/regression.md`
4. 릴리즈 QA 리포트 `docs/qa/reports/vX.Y.Z.md`
5. 발견 이슈는 GitHub Issue 또는 티켓으로 등록

## 심각도 정의

| 레벨 | 정의 | 예시 |
|------|------|------|
| Critical | 서비스 중단·데이터 손실·보안 침해 | 인증 없이 제보 API 접근, JWT 검증 우회 |
| High | 핵심 기능 작동 불가 | 화장실 검색 결과 미반환, 로그인 전체 실패 |
| Medium | 일부 플로우 제약, 우회 가능 | 특정 반경 값에서 빈 결과 반환 |
| Low | UI·문서 미세 결함 | Swagger 주석 누락, 응답 메시지 오탈자 |

## 커버리지 목표

- 백엔드 라인 커버리지 80%+
- 앱 핵심 플로우 커버리지 70%+
- E2E: MVP 핵심 유저 저니 5개 이상 자동화

---

## 핵심 테스트 시나리오 (MVP)

### SC-01. 비로그인 화장실 검색

```
전제: MySQL에 테스트 화장실 데이터 3건 존재
1. GET /api/v1/toilets?lat=37.5&lng=127.0&radiusMeters=500
2. 200 OK + 반경 내 화장실 목록 반환
3. 응답 포맷: ApiResponse<List<ToiletResponse>> 준수
4. 반경 외 화장실 미포함 검증
```

### SC-02. 화장실 상세 조회

```
1. GET /api/v1/toilets/{id} (존재하는 ID)
2. 200 OK + 상세 정보 반환
3. GET /api/v1/toilets/{id} (존재하지 않는 ID)
4. 404 Not Found + ErrorCode.TOILET_NOT_FOUND
```

### SC-03. Kakao/Apple OAuth 로그인

```
1. POST /api/v1/auth/login { provider: "KAKAO", accessToken: "..." }
2. Kakao API mock → 사용자 정보 반환
3. 신규 사용자: UserEntity upsert + AccessToken + RefreshToken 반환
4. 기존 사용자: 동일 사용자 ID로 토큰 재발급
5. 만료된 accessToken: 401 Unauthorized
```

### SC-04. 로그인 후 화장실 제보

```
전제: 유효한 Bearer JWT
1. POST /api/v1/toilets/report { name, address, lat, lng, male, female, disabled }
2. 201 Created + ToiletResponse (status: PENDING)
3. DB: ToiletEntity.status = PENDING, reportedBy = userId
4. 인증 없이 동일 요청 → 401 Unauthorized
```

### SC-05. Refresh Token 갱신

```
1. POST /api/v1/auth/refresh { refreshToken: "..." }
2. 유효한 refreshToken → 새 AccessToken 반환
3. 만료된 refreshToken → 401 Unauthorized
4. Redis에서 이미 무효화된 refreshToken → 401
```

### SC-06. 보안 기본 체크

```
- Authorization 헤더 없이 /api/v1/toilets/report 접근 → 401
- 타인의 제보 데이터 접근 시도 → 403 (Phase 2)
- 입력값 초과 (lat > 90, lng > 180) → 400 Bad Request
- SQL 주입 패턴 포함 요청 → 400 또는 QueryDSL 파라미터 바인딩으로 무해화 확인
```

---

## API 통합 테스트 가이드 (Testcontainers)

```kotlin
// Testcontainers + SpringBootTest 패턴
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ToiletControllerIntegrationTest {

    companion object {
        @Container
        val mysql = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("geuphalttaen_test")
            withUsername("test")
            withPassword("test")
        }

        @Container
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }
    }
    // ...
}
```

- MySQL 이미지: `mysql:8.0` 고정
- Redis 이미지: `redis:7-alpine`
- 컨테이너 재사용: `.testcontainers.properties`에 `testcontainers.reuse.enable=true`

---

## 앱 E2E 테스트 (Maestro)

- 도구: [Maestro](https://maestro.mobile.dev/) (iOS/Android 공통)
- 시나리오 위치: `geuphalttaen-new-app/e2e/maestro/`
- 주요 플로우:
  - 비로그인 지도 조회 플로우
  - Kakao 로그인 플로우
  - 화장실 제보 플로우

```yaml
# 예시: 비로그인 화장실 검색
appId: com.geuphalttaen.app
---
- launchApp
- assertVisible: "내 주변 화장실"
- tapOn: id: "map_view"
- assertVisible: id: "toilet_marker"
```

---

## 성능·부하 테스트 (k6)

- 화장실 검색 API: 동시 50 VU, 5분, p95 < 500ms 목표
- 로그인 API: 동시 20 VU, 2분
- 부하 시나리오: `docs/qa/load/k6_scenarios.js`

---

## 서브 에이전트

| 서브 에이전트 | 역할 | 주 수정 영역 |
|--------------|------|-------------|
| 시나리오 설계 | 테스트 케이스 설계 (동등 분할·경계값·상태 전이), 우선순위 분류 | `docs/qa/scenarios.md` |
| 통합·회귀 | API 통합 테스트 (Testcontainers), 회귀 매트릭스 유지 | `geuphalttaen-*/src/test/kotlin/**` |
| 앱 E2E | Maestro 플로우 자동화 (비로그인 조회·로그인·제보) | `geuphalttaen-new-app/e2e/maestro/` |
| 보안 테스트 | OWASP Top 10·인증·인가·입력 검증·레이트 리밋 체크리스트 | `docs/qa/security-checklist.md` |
| 성능·부하 | k6 시나리오, p95 추적, 병목 프로파일링 | `docs/qa/load/` |
| QA 리포트 | 릴리즈별 QA 리포트, 심각도 분류, 버그 티켓 등록·추적 | `docs/qa/reports/**` |
