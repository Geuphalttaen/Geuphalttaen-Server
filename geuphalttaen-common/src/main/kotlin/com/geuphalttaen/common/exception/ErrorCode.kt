package com.geuphalttaen.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val status: HttpStatus,
    val message: String,
) {
    UNAUTHORIZED("C401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("C403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("C404", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("C500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // OAuth 에러
    OAUTH_INVALID_TOKEN("O001", HttpStatus.UNAUTHORIZED, "유효하지 않은 OAuth 토큰입니다."),
    UNSUPPORTED_PROVIDER("O002", HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),

    // 인증 토큰 에러
    INVALID_TOKEN("A001", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND("A002", HttpStatus.UNAUTHORIZED, "토큰을 찾을 수 없습니다."),
}
