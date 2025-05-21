# 📘 Logging Convention Guide

> 이 문서는 프로젝트의 로깅 일관성과 가시성을 높이기 위한 기준을 정의합니다.
> 계층별 로깅 위치와 로그 메시지 형식을 명확히 해, 디버그를 포함한 운영 중 문제 추적을 유용하게 합니다.

---

## 💡 로깅 기본 원칙

* **로깅은 흐름을 따라 읽을 수 있어야 한다.**
* **누가(userId), 언제, 어디서, 무엇을 했는지**가 드러나야 한다.
* 로깅 레벨은 목적에 따라 구분한다: `INFO`, `WARN`, `ERROR`
* 민감 정보(비밀번호, 신체정보 등)는 절대 기록하지 않는다.

---

## ✅ 로깅 레벨 기준

| 레벨      | 사용 목적                 | 예시                      |
| ------- |-----------------------|-------------------------|
| `INFO`  | 정상 흐름 추적   | 로그인 요청, 토큰 발급, 유저 등록 완료 |
| `WARN`  | 경고 상황, 오류는 아닌 데 주의 필요 | 조건 불충족, 재시도 발생          |
| `ERROR` | 예외 상황 및 실패  | API 호출 실패, 내부 예외        |

---

## ✅ 계층별 로깅 가이드

### 1. **Controller**

* API 요청 수신 지점
* 주요 쿼리 파라미터 또는 경로 변수 기록

```java
// 로그인 요청
log.info("[POST /api/v1/auth/kakao] 카카오 로그인 요청 수신");

// 리포트 조회
log.info("[GET /api/v1/reports/weekly] 주간 리포트 조회 요청 - userId={}, year={}, month={}, week={}",
         userDetails.getUserId(), targetYear, targetMonth, targetWeek);
```

---

### 2. **Service**

* 로직 시작/완료
* 주요 비즈니스 성공/실패 흐름
* 외부 API 호출 전후

```java
log.info("[AuthService.login] 로그인 처리 시작");
log.info("[AuthService.login] JWT 발급 완료 - userId={}", userId);
log.warn("[AuthService.login] 기존 사용자 조회 실패 - oauthId={}", oauthId);
log.error("[AuthService.login] 카카오 API 호출 실패 - message={}", e.getMessage(), e);
```

---

### 3. **Client (외부 API 호출 등)**

* 호출 시작/응답 완료
* 실패 시 응답 코드 및 메시지

```java
log.info("[KakaoClient.getToken] 카카오 accessToken 요청 시작 ");
log.error("[KakaoClient.getUserInfo] 카카오 API 응답 오류 - status={}, message={}", statusCode, responseMessage);
```

---

### 4. **Exception Handler**

* 전역 혹은 개별 예외 발생 시
* URI, 에러 메시지, 스택 트레이스 포함

```java
log.error("[GlobalExceptionHandler] 로그인 처리 중 예외 발생 - uri={}, message={}", requestURI, e.getMessage(), e);
```

---

## ✅ 로깅 메시지 형식

```text
[클래스명.메서드명] 동작 설명 - 주요 파라미터
```

예시:

```text
[AuthService.login] JWT 발급 완료 - userId=42
```

---

## ✅ MDC (Mapped Diagnostic Context) 활용

* 요청 단위 식별자(userId, requestId 등)를 MDC에 저장하고, Logback 패턴에서 `%X{...}` 형식을 사용해 로그에 자동 포함시킬 수 있습니다.

예시 로그 출력:

```text
2025-05-21 17:53:00.812  INFO 12345 --- [http-nio-1] [userId=42] AuthController : [POST /api/v1/auth/logout] 로그아웃 요청 수신
```

---

## ✅ 로깅 금지 항목

* 건강 정보, 카페인 정보 등 유저의 개인정보
* 인증 토큰 (`access_token`, `refresh_token`)
* 요청/응답 JSON 바디 전체 (필요 시 요약)

---

## ✅ 권장 로깅 흐름 예시 (소셜 로그인)

```text
[INFO] [POST /api/v1/auth/kakao] 카카오 로그인 요청 수신
[INFO] [AuthService.login] 카카오 로그인 로직 시작
[INFO] [KakaoClient.getToken] accessToken 요청
[INFO] [KakaoClient.getUserInfo] 유저 정보 조회 시작
[INFO] [AuthService.login] JWT 발급 완료 - userId=42
```

---

## ✅ 참고사항

* 개발 환경에서는 `DEBUG` 수준 로깅도 허용
* 운영 환경에서는 `INFO` 이상만 출력
* 필요시 `traceId`, `sessionId` 등 추가 가능
